from __future__ import annotations

import logging
from collections.abc import Callable
from contextvars import ContextVar
from math import isclose
from typing import Any, Literal, TypedDict
from uuid import UUID

from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, ValidationError

from app.agent_client import AgentClient
from app.agent_types import AgentAttempt, AgentCallError, AgentInvocation
from app.assessment_report import SAFE_FAILURE_MESSAGE, completed_assessment, failed_assessment
from app.config import Settings
from app.models import (
    EvidenceResponse,
    GradingResponse,
    OutboxEvent,
    ReviewDecision,
    ReviewResponse,
    SuggestionResponse,
    SuggestionStatus,
    TERMINAL_WORKFLOW_STATUSES,
    WorkflowClaim,
    WorkflowRecord,
    WorkflowStatus,
    WorkflowSubmission,
)
from app.observability import (
    record_agent_call,
    record_agent_contract_rejection,
    record_workflow_budget_guard,
)
from app.repository import WorkflowRepository
from app.security import redact_for_trace
from app.workflow_progress import ProgressEvent, progress_event
from app.workflow_telemetry import agent_attempt_entries, response_metadata
from app.workflow_validation import validate_submission

LOGGER = logging.getLogger(__name__)
ProgressReporter = Callable[[ProgressEvent], None]
_ACTIVE_CLAIM: ContextVar[WorkflowClaim | None] = ContextVar("active_workflow_claim", default=None)
_REVIEW_REASSESSMENT_REASONS = {
    ReviewDecision.REGRADE: "REVIEW_REGRADE",
    ReviewDecision.REJECT: "REVIEW_REJECT",
}


class GraphState(TypedDict, total=False):
    workflow_id: UUID
    submission: WorkflowSubmission
    evidence: dict[str, Any]
    grading: dict[str, Any]
    review: dict[str, Any]
    suggestions: dict[str, Any]
    grading_passes: int
    reassessment_reason: str
    error: str
    completion_event: dict[str, Any]
    progress_reporter: ProgressReporter | None
    resume_status: WorkflowStatus
    claim: WorkflowClaim | None


class OrchestrationWorkflow:
    """Deterministic LangGraph coordinator; providers and presentation live elsewhere."""

    def __init__(
        self,
        settings: Settings,
        repository: WorkflowRepository,
        agent_client: AgentClient,
        progress_reporter: ProgressReporter | None = None,
    ):
        self.settings = settings
        self.repository = repository
        self.agent_client = agent_client
        self.progress_reporter = progress_reporter
        self.graph = self._build_graph()

    def run(
        self,
        record: WorkflowRecord,
        progress_reporter: ProgressReporter | None = None,
        claim: WorkflowClaim | None = None,
    ) -> WorkflowRecord:
        if record.status in TERMINAL_WORKFLOW_STATUSES:
            return record
        state: GraphState = {
            "workflow_id": record.workflow_id,
            "submission": WorkflowSubmission.model_validate(record.input_payload),
            "evidence": record.evidence_output or {},
            "grading": record.grading_output or {},
            "review": record.review_output or {},
            "suggestions": record.suggestion_output or {},
            "grading_passes": record.grading_passes,
            "progress_reporter": progress_reporter or self.progress_reporter,
            "resume_status": record.status,
            "claim": claim,
        }
        claim_token = _ACTIVE_CLAIM.set(claim)
        try:
            self.graph.invoke(state)
        finally:
            _ACTIVE_CLAIM.reset(claim_token)
        return self.repository.get(record.workflow_id)

    def _build_graph(self):
        graph = StateGraph(GraphState)
        for name, handler in {
            "validate": self._validate,
            "evidence": self._evidence,
            "grade": self._grade,
            "review": self._review,
            "suggest": self._suggest,
            "assemble": self._assemble,
            "fail": self._fail,
        }.items():
            graph.add_node(name, handler)
        graph.add_conditional_edges(
            START,
            self._resume_from_status,
            {"validate": "validate", "evidence": "evidence", "grade": "grade", "review": "review", "suggest": "suggest", "assemble": "assemble", "fail": "fail"},
        )
        graph.add_conditional_edges("validate", self._after_validate, {"evidence": "evidence", "fail": "fail"})
        graph.add_conditional_edges("evidence", self._after_evidence, {"grade": "grade", "fail": "fail"})
        graph.add_conditional_edges("grade", self._after_grade, {"evidence": "evidence", "review": "review", "fail": "fail"})
        graph.add_conditional_edges("review", self._after_review, {"evidence": "evidence", "suggest": "suggest", "fail": "fail"})
        graph.add_conditional_edges("suggest", self._after_suggest, {"assemble": "assemble", "fail": "fail"})
        graph.add_edge("assemble", END)
        graph.add_edge("fail", END)
        return graph.compile()

    @staticmethod
    def _resume_from_status(state: GraphState) -> Literal["validate", "evidence", "grade", "review", "suggest", "assemble", "fail"]:
        status = state["resume_status"]
        if status is WorkflowStatus.RECEIVED:
            return "validate"
        if status is WorkflowStatus.EVIDENCING:
            return "evidence"
        if status in {WorkflowStatus.GRADING, WorkflowStatus.REGRADING}:
            return "grade"
        if status is WorkflowStatus.REVIEWING:
            return "review"
        if status is WorkflowStatus.SUGGESTING:
            return "assemble" if state.get("suggestions") else "suggest"
        return "fail"

    def _validate(self, state: GraphState) -> GraphState:
        error = validate_submission(state["submission"], self.settings)
        if error:
            return {"error": error}
        try:
            provider_details = self.agent_client.ensure_ready(
                include_suggestions=state["submission"].composition.language != "CHINESE"
            )
        except Exception as provider_error:
            return {"error": f"assessment provider is unavailable: {redact_for_trace(provider_error)}"}
        self._update(
            state["workflow_id"],
            log_step="VALIDATED",
            log_details={"agentProvider": self.settings.agent_provider, "agentCards": provider_details or {}},
        )
        return {}

    @staticmethod
    def _after_validate(state: GraphState) -> Literal["evidence", "fail"]:
        return "fail" if state.get("error") else "evidence"

    def _evidence(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        attempt = state["grading_passes"] + 1
        self._update(
            workflow_id,
            status=WorkflowStatus.EVIDENCING,
            log_step="EVIDENCE_STARTED",
            attempt=attempt,
            outbox=self._progress_outbox(state, WorkflowStatus.EVIDENCING, attempt),
        )
        self._report_progress(state, WorkflowStatus.EVIDENCING)
        try:
            evidence, invocation = self._invoke_agent(
                "evidence",
                "evidence",
                state["submission"].request_payload(workflow_id, attempt),
                EvidenceResponse,
                workflow_id,
                attempt,
                state["submission"],
            )
        except AgentCallError as error:
            return {"error": f"evidence collection failed: {redact_for_trace(error)}"}
        next_status = WorkflowStatus.REGRADING if attempt > 1 else WorkflowStatus.GRADING
        self._update(
            workflow_id,
            status=next_status,
            log_step="EVIDENCE_COMPLETED",
            attempt=attempt,
            evidence_output=evidence,
            log_details=response_metadata(evidence, invocation),
        )
        return {"evidence": evidence, "reassessment_reason": None}

    @staticmethod
    def _after_evidence(state: GraphState) -> Literal["grade", "fail"]:
        return "fail" if state.get("error") else "grade"

    def _grade(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        attempt = state["grading_passes"] + 1
        status = WorkflowStatus.REGRADING if attempt > 1 else WorkflowStatus.GRADING
        self._update(
            workflow_id,
            status=status,
            log_step="GRADING_STARTED",
            attempt=attempt,
            outbox=self._progress_outbox(state, status, attempt),
            **({"review_output": {}} if attempt > 1 else {}),
        )
        self._report_progress(state, status)
        try:
            payload = state["submission"].request_payload(workflow_id, attempt)
            payload["evidence"] = state["evidence"]
            # Evidence obtains the rubric once; the persisted workflow snapshot is
            # authoritative for every downstream assessment decision.
            payload["rubricSnapshot"] = state["evidence"]["rubricSnapshot"]
            grading, invocation = self._invoke_agent(
                "grade",
                "grading",
                payload,
                GradingResponse,
                workflow_id,
                attempt,
                state["submission"],
            )
        except AgentCallError as error:
            return {"error": f"grading failed: {redact_for_trace(error)}", "grading_passes": attempt}
        snapshot_error = self._grading_snapshot_error(grading, state["evidence"])
        if snapshot_error:
            record_agent_contract_rejection("grading", snapshot_error)
            return {
                "error": "grading response did not match the workflow rubric snapshot",
                "grading": grading,
                "grading_passes": attempt,
            }
        if grading["confidence"] < self.settings.minimum_confidence:
            if self._can_reassess(attempt):
                self._request_reassessment(
                    state,
                    "LOW_GRADING_CONFIDENCE",
                    grading_output=grading,
                    grading_attempt=attempt,
                    log_details=response_metadata(grading, invocation),
                )
                return {
                    "grading": grading,
                    "review": {},
                    "grading_passes": attempt,
                    "reassessment_reason": "LOW_GRADING_CONFIDENCE",
                }
            return {
                "error": "grading confidence remained below the reassessment threshold",
                "grading": grading,
                "grading_passes": attempt,
            }
        self._update(
            workflow_id,
            status=WorkflowStatus.REVIEWING,
            log_step="GRADING_COMPLETED",
            attempt=attempt,
            grading_output=grading,
            grading_passes=attempt,
            log_details=response_metadata(grading, invocation),
            outbox=self._progress_outbox(state, WorkflowStatus.REVIEWING, attempt),
        )
        self._report_progress(state, WorkflowStatus.REVIEWING)
        return {"grading": grading, "review": {}, "grading_passes": attempt, "reassessment_reason": None}

    @staticmethod
    def _grading_snapshot_error(grading: dict[str, Any], evidence: dict[str, Any]) -> str | None:
        """Reject a grading artifact that was produced against a different rubric."""
        snapshot = evidence.get("rubricSnapshot")
        if not isinstance(snapshot, dict):
            return "missing_rubric_snapshot"
        version = snapshot.get("version")
        criteria = snapshot.get("criteria")
        if not isinstance(version, str) or not isinstance(criteria, list):
            return "invalid_rubric_snapshot"
        expected = {item.get("name") for item in criteria if isinstance(item, dict)}
        actual_criteria = {
            item.get("criterion") for item in grading.get("criteria", []) if isinstance(item, dict)
        }
        score_names = set(grading.get("rubricScores", {}))
        if grading.get("rubricVersion") != version:
            return "rubric_version_mismatch"
        if not expected or actual_criteria != expected or score_names != expected:
            return "rubric_criteria_mismatch"
        weights = {item["name"]: item.get("weight") for item in criteria if isinstance(item, dict) and "name" in item}
        scores = {item["criterion"]: item for item in grading["criteria"]}
        try:
            expected_overall = 0.0
            for criterion in expected:
                score = float(scores[criterion]["score"])
                band_score = int(scores[criterion]["bandScore"])
                weight = float(weights[criterion])
                if not isclose(score, band_score * 20.0, abs_tol=0.01):
                    return "rubric_score_scale_mismatch"
                if not isclose(float(grading["rubricScores"][criterion]), score, abs_tol=0.01):
                    return "rubric_score_mismatch"
                expected_overall += score * weight / 100.0
            if not isclose(float(grading["overallScore"]), expected_overall, abs_tol=0.01):
                return "overall_score_mismatch"
        except (KeyError, TypeError, ValueError):
            return "invalid_rubric_score"
        return None

    @staticmethod
    def _after_grade(state: GraphState) -> Literal["evidence", "review", "fail"]:
        if state.get("error"):
            return "fail"
        return "evidence" if state.get("reassessment_reason") else "review"

    def _review(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        if state.get("review"):
            return {"review": state["review"]}
        payload = state["submission"].request_payload(workflow_id, state["grading_passes"], state["grading"])
        payload.update({
            "evidence": state["evidence"],
            "grading": state["grading"],
            "rubricSnapshot": state["evidence"]["rubricSnapshot"],
        })
        try:
            review, invocation = self._invoke_agent(
                "review", "review", payload, ReviewResponse, workflow_id, state["grading_passes"], state["submission"]
            )
        except AgentCallError as error:
            return {"error": f"review failed: {redact_for_trace(error)}"}
        self._update(
            workflow_id,
            status=WorkflowStatus.REVIEWING,
            log_step="REVIEW_COMPLETED",
            attempt=state["grading_passes"],
            review_output=review,
            log_details=response_metadata(review, invocation),
        )
        return {"review": review}

    def _after_review(self, state: GraphState) -> Literal["evidence", "suggest", "fail"]:
        if state.get("error"):
            return "fail"
        decision = ReviewDecision(state["review"]["decision"])
        reassessment_reason = self._review_reassessment_reason(state["review"], decision)
        if reassessment_reason is not None:
            return self._reassess_or_fail(state, reassessment_reason)
        if decision is ReviewDecision.APPROVE:
            self._update(
                state["workflow_id"],
                status=WorkflowStatus.SUGGESTING,
                log_step="REVIEW_APPROVED",
                attempt=state["grading_passes"],
                outbox=self._progress_outbox(state, WorkflowStatus.SUGGESTING, state["grading_passes"]),
            )
            self._report_progress(state, WorkflowStatus.SUGGESTING)
            return "suggest"
        return "fail"

    def _review_reassessment_reason(self, review: dict[str, Any], decision: ReviewDecision) -> str | None:
        if review["confidence"] < self.settings.minimum_confidence:
            return "LOW_REVIEW_CONFIDENCE"
        return _REVIEW_REASSESSMENT_REASONS.get(decision)

    def _reassess_or_fail(self, state: GraphState, reason: str) -> Literal["evidence", "fail"]:
        if self._can_reassess(state["grading_passes"]):
            self._request_reassessment(state, reason)
            return "evidence"
        self._record_reassessment_limit(state, reason)
        return "fail"

    def _can_reassess(self, completed_grading_passes: int) -> bool:
        """Keep the configured cap in regrades, rather than total grading attempts."""
        return completed_grading_passes <= self.settings.max_regrades

    def _request_reassessment(
        self,
        state: GraphState,
        reason: str,
        *,
        grading_output: dict[str, Any] | None = None,
        grading_attempt: int | None = None,
        log_details: dict[str, Any] | None = None,
    ) -> None:
        """Record a student-safe, bounded reassessment using existing peer signals.

        The coordinator never decides whether the rubric was applied correctly; it
        merely routes low-confidence or REVIEW_REGRADE results through fresh
        evidence, grading, and review.
        """
        attempt = grading_attempt if grading_attempt is not None else state["grading_passes"]
        details = {"reassessmentReason": reason}
        if log_details:
            details.update(log_details)
        fields: dict[str, Any] = {
            # Persist the next durable node, so a worker crash resumes with fresh
            # evidence rather than grading again from the prior evidence artifact.
            "status": WorkflowStatus.EVIDENCING,
            "log_step": "REASSESSMENT_REQUESTED",
            "attempt": attempt + 1,
            "review_output": {},
            "log_details": details,
            "outbox": self._progress_outbox(state, WorkflowStatus.REGRADING, attempt + 1),
        }
        if grading_output is not None:
            fields["grading_output"] = grading_output
        if grading_attempt is not None:
            fields["grading_passes"] = grading_attempt
        self._update(state["workflow_id"], **fields)
        self._report_progress(state, WorkflowStatus.REGRADING)

    def _record_reassessment_limit(self, state: GraphState, reason: str) -> None:
        self._update(
            state["workflow_id"],
            status=WorkflowStatus.REVIEWING,
            log_step="REASSESSMENT_LIMIT_REACHED",
            attempt=state["grading_passes"],
            log_details={"reassessmentReason": reason},
        )

    def _suggest(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        if state.get("suggestions"):
            return {"suggestions": state["suggestions"]}
        if state["submission"].composition.language == "CHINESE":
            # Chinese grading/review remains available, but this English-only agent
            # must not turn a completed assessment into a workflow failure.
            return self._persist_empty_suggestions(
                state,
                SuggestionStatus.NOT_AVAILABLE_FOR_LANGUAGE,
                "SUGGESTION_SKIPPED_LANGUAGE",
            )
        payload = state["submission"].request_payload(workflow_id, state["grading_passes"], state["grading"])
        payload.update({
            "grading": state["grading"],
            "review": state["review"],
            # Suggestions must rank against the same framework Evidence captured for this
            # workflow; it must never refetch a potentially edited Admin rubric.
            "rubricSnapshot": state["evidence"]["rubricSnapshot"],
        })
        try:
            suggestions, invocation = self._invoke_agent(
                "suggest", "suggestion", payload, SuggestionResponse, workflow_id, state["grading_passes"], state["submission"]
            )
        except AgentCallError as error:
            # Suggestions enhance an approved assessment; an unavailable peer must not
            # discard the approved score, evidence, and feedback the learner can use.
            LOGGER.warning("suggestion_generation_unavailable workflow_id=%s reason=%s", workflow_id, redact_for_trace(error))
            return self._persist_empty_suggestions(
                state,
                SuggestionStatus.TEMPORARILY_UNAVAILABLE,
                "SUGGESTION_UNAVAILABLE",
            )
        validation_error = self._suggestion_validation_error(
            suggestions,
            state["grading"],
            state["submission"].composition.content,
        )
        if validation_error is not None:
            LOGGER.warning(
                "suggestion_generation_rejected workflow_id=%s reason=%s",
                workflow_id,
                validation_error,
            )
            record_agent_contract_rejection("suggestion", validation_error.casefold())
            return self._persist_empty_suggestions(
                state,
                SuggestionStatus.TEMPORARILY_UNAVAILABLE,
                "SUGGESTION_REJECTED_INCONSISTENT",
                {"validationReason": validation_error},
            )
        self._update(
            workflow_id,
            status=WorkflowStatus.SUGGESTING,
            log_step="SUGGESTION_COMPLETED",
            attempt=state["grading_passes"],
            suggestion_output=suggestions,
            log_details=response_metadata(suggestions, invocation),
        )
        return {"suggestions": suggestions}

    @staticmethod
    def _suggestion_validation_error(
        suggestions: dict[str, Any],
        grading: dict[str, Any],
        composition_content: str,
    ) -> str | None:
        """Return a bounded reason when peer coaching conflicts with approved grading."""
        assessments = {
            item["criterion"].casefold(): item
            for item in grading["criteria"]
        }
        status = SuggestionStatus(suggestions["suggestionStatus"])
        if status is SuggestionStatus.NO_PRIORITY_IMPROVEMENT:
            return None if all(item["score"] >= 100 for item in assessments.values()) else "NO_PRIORITY_WITH_IMPERFECT_CRITERION"
        if status is not SuggestionStatus.AVAILABLE:
            return None
        for suggestion in suggestions["suggestions"]:
            assessment = assessments.get(suggestion["criterion"].casefold())
            if assessment is None:
                return "UNSUPPORTED_CRITERION"
            if assessment["score"] >= 100:
                return "PERFECT_CRITERION"
            source_excerpt = suggestion["supportingEvidence"]["excerpt"]
            example = suggestion["exampleRevision"]
            if source_excerpt not in composition_content:
                return "EVIDENCE_NOT_IN_COMPOSITION"
            if OrchestrationWorkflow._normalise_coaching_text(example) == OrchestrationWorkflow._normalise_coaching_text(source_excerpt):
                return "NO_OP_EXAMPLE"
        return None

    @staticmethod
    def _normalise_coaching_text(value: str) -> str:
        return "".join(character for character in value.casefold() if character.isalnum())

    def _persist_empty_suggestions(
        self,
        state: GraphState,
        suggestion_status: SuggestionStatus,
        log_step: str,
        log_details: dict[str, Any] | None = None,
    ) -> GraphState:
        suggestions = SuggestionResponse(
            suggestionStatus=suggestion_status,
            suggestions=[],
        ).model_dump(by_alias=True, mode="json")
        self._update(
            state["workflow_id"],
            status=WorkflowStatus.SUGGESTING,
            log_step=log_step,
            attempt=state["grading_passes"],
            suggestion_output=suggestions,
            log_details=log_details,
        )
        return {"suggestions": suggestions}

    @staticmethod
    def _after_suggest(state: GraphState) -> Literal["assemble", "fail"]:
        return "fail" if state.get("error") else "assemble"

    def _assemble(self, state: GraphState) -> GraphState:
        grading = GradingResponse.model_validate(state["grading"])
        review = ReviewResponse.model_validate(state["review"])
        suggestions = SuggestionResponse.model_validate(state["suggestions"])
        report, completion = completed_assessment(state["submission"], state["workflow_id"], grading, review, suggestions)
        self._update(
            state["workflow_id"],
            status=WorkflowStatus.COMPLETED,
            log_step="ASSEMBLED",
            final_report=report,
            completion_event=completion,
            outbox=self._completion_outbox(state, completion),
        )
        return {"completion_event": completion}

    def _fail(self, state: GraphState) -> GraphState:
        completion = failed_assessment(state["submission"], state["workflow_id"])
        reason = state.get("error", "review did not approve the assessment")
        self._record_failure(
            state["workflow_id"],
            {"message": redact_for_trace(reason)},
            completion,
            self._completion_outbox(state, completion),
        )
        LOGGER.warning("workflow_failed workflow_id=%s reason=%s", state["workflow_id"], reason)
        return {"completion_event": completion}

    def _invoke_agent(
        self,
        operation: Literal["evidence", "grade", "review", "suggest"],
        agent: str,
        payload: dict[str, Any],
        parser: type[BaseModel],
        workflow_id: UUID,
        grading_pass: int,
        submission: WorkflowSubmission,
    ) -> tuple[dict[str, Any], AgentInvocation[Any]]:
        try:
            spent = self._workflow_cost_micros(workflow_id)
            if spent >= self.settings.max_workflow_cost_micros:
                record_workflow_budget_guard()
                raise AgentCallError(agent, "workflow AI spend budget is exhausted", ())
            invocation = getattr(self.agent_client, operation)(payload, parser)
            result = invocation.response
            if result.usage is None and self.settings.agent_provider == "a2a":
                # A production response without provider usage would make cost reporting silently incomplete.
                raise AgentCallError(
                    agent,
                    f"{agent} omitted the required provider usage",
                    invocation.attempts + (AgentAttempt(len(invocation.attempts) + 1, "MISSING_USAGE", 0),),
                )
            self._log_agent_attempts(workflow_id, agent, grading_pass, agent_attempt_entries(agent, list(invocation.attempts)))
            if result.usage is not None:
                self.repository.record_agent_usage(
                    workflow_id=workflow_id,
                    request_id=submission.completion_request_id(workflow_id),
                    user_id=submission.composition.user_id,
                    composition_id=submission.composition.composition_id,
                    agent=agent,
                    attempt=grading_pass,
                    usage=result.usage,
                    invocation_id=invocation.task_id,
                    **self._claim_kwargs(),
                )
            response = result.model_dump(by_alias=True, mode="json")
            record_agent_call(agent, True, sum(item.latency_ms for item in invocation.attempts))
            return response, invocation
        except AgentCallError as error:
            self._record_contract_rejection(agent, error)
            record_agent_call(agent, False, sum(item.latency_ms for item in error.attempts))
            self._log_agent_attempts(workflow_id, agent, grading_pass, agent_attempt_entries(agent, list(error.attempts)))
            raise
        except ValidationError as error:
            record_agent_contract_rejection(agent, "invalid_response")
            record_agent_call(agent, False)
            raise AgentCallError(agent, f"{agent} returned an invalid response") from error

    def _workflow_cost_micros(self, workflow_id: UUID) -> int:
        cost_reader = getattr(self.repository, "workflow_cost_micros", None)
        return int(cost_reader(workflow_id)) if cost_reader is not None else 0

    @staticmethod
    def _record_contract_rejection(agent: str, error: AgentCallError) -> None:
        statuses = {attempt.status for attempt in error.attempts}
        if "MISSING_USAGE" in statuses:
            record_agent_contract_rejection(agent, "missing_usage")
        elif "INVALID_RESPONSE" in statuses:
            record_agent_contract_rejection(agent, "invalid_response")
        elif "PROTOCOL_ERROR" in statuses:
            record_agent_contract_rejection(agent, "protocol_error")

    @staticmethod
    def _report_progress(state: GraphState, status: WorkflowStatus) -> None:
        reporter = state.get("progress_reporter")
        event = progress_event(state["workflow_id"], state["submission"], status)
        if reporter is not None and event is not None:
            reporter(event)

    def _update(self, workflow_id: UUID, **fields: Any) -> WorkflowRecord:
        """Apply a transition only while this runner still holds its fencing lease."""
        if _ACTIVE_CLAIM.get() is None:
            # Keep the graph directly testable without requiring a database outbox.
            fields.pop("outbox", None)
        return self.repository.update(workflow_id, **fields, **self._claim_kwargs())

    def _record_failure(
        self,
        workflow_id: UUID,
        error_details: dict[str, Any],
        completion_event: dict[str, Any],
        outbox: OutboxEvent,
    ) -> WorkflowRecord:
        if _ACTIVE_CLAIM.get() is None:
            return self.repository.record_failure(workflow_id, error_details, completion_event)
        return self.repository.record_failure(
            workflow_id,
            error_details,
            completion_event,
            outbox=outbox,
            **self._claim_kwargs(),
        )

    def _log_agent_attempts(self, workflow_id: UUID, agent: str, attempt: int, entries: list[dict[str, Any]]) -> None:
        self.repository.log_agent_attempts(workflow_id, agent, attempt, entries, **self._claim_kwargs())

    def _claim_kwargs(self) -> dict[str, WorkflowClaim]:
        # Unit tests run the graph without a durable worker. Production calls always
        # set this per-run value before invoking the graph.
        claim = _ACTIVE_CLAIM.get()
        return {"claim": claim} if claim is not None else {}

    def _progress_outbox(self, state: GraphState, status: WorkflowStatus, attempt: int) -> OutboxEvent | None:
        event = progress_event(state["workflow_id"], state["submission"], status)
        if event is None:
            return None
        return OutboxEvent(
            workflow_id=state["workflow_id"],
            event_type="PROGRESS",
            dedupe_key=f"{state['workflow_id']}:progress:{status.value}:{attempt}",
            topic=self.settings.grading_progress_topic,
            key=str(event["compositionId"]),
            payload=event,
        )

    def _completion_outbox(self, state: GraphState, completion: dict[str, Any]) -> OutboxEvent:
        return OutboxEvent(
            workflow_id=state["workflow_id"],
            event_type="COMPLETION",
            dedupe_key=f"{state['workflow_id']}:completion",
            topic=self.settings.grading_completed_topic,
            key=str(completion["compositionId"]),
            payload=completion,
        )
