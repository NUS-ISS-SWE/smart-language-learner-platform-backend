from __future__ import annotations

import logging
from collections.abc import Callable
from typing import Any, Literal, TypedDict
from uuid import UUID

from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, ValidationError

from app.agent_client import AgentClient
from app.agent_types import AgentCallError, AgentInvocation
from app.assessment_report import SAFE_FAILURE_MESSAGE, completed_assessment, failed_assessment
from app.config import Settings
from app.models import (
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
from app.observability import record_agent_call
from app.repository import WorkflowRepository
from app.security import redact_for_trace
from app.workflow_progress import ProgressEvent, progress_event
from app.workflow_telemetry import agent_attempt_entries, response_metadata
from app.workflow_validation import validate_submission

LOGGER = logging.getLogger(__name__)
ProgressReporter = Callable[[ProgressEvent], None]


class GraphState(TypedDict, total=False):
    workflow_id: UUID
    submission: WorkflowSubmission
    grading: dict[str, Any]
    review: dict[str, Any]
    suggestions: dict[str, Any]
    grading_passes: int
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
            "grading": record.grading_output or {},
            "review": record.review_output or {},
            "suggestions": record.suggestion_output or {},
            "grading_passes": record.grading_passes,
            "progress_reporter": progress_reporter or self.progress_reporter,
            "resume_status": record.status,
            "claim": claim,
        }
        previous_claim = getattr(self, "_active_claim", None)
        self._active_claim = claim
        try:
            self.graph.invoke(state)
        finally:
            self._active_claim = previous_claim
        return self.repository.get(record.workflow_id)

    def _build_graph(self):
        graph = StateGraph(GraphState)
        for name, handler in {
            "validate": self._validate,
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
            {"validate": "validate", "grade": "grade", "review": "review", "suggest": "suggest", "assemble": "assemble", "fail": "fail"},
        )
        graph.add_conditional_edges("validate", self._after_validate, {"grade": "grade", "fail": "fail"})
        graph.add_conditional_edges("grade", self._after_grade, {"review": "review", "fail": "fail"})
        graph.add_conditional_edges("review", self._after_review, {"grade": "grade", "suggest": "suggest", "fail": "fail"})
        graph.add_conditional_edges("suggest", self._after_suggest, {"assemble": "assemble", "fail": "fail"})
        graph.add_edge("assemble", END)
        graph.add_edge("fail", END)
        return graph.compile()

    @staticmethod
    def _resume_from_status(state: GraphState) -> Literal["validate", "grade", "review", "suggest", "assemble", "fail"]:
        status = state["resume_status"]
        if status is WorkflowStatus.RECEIVED:
            return "validate"
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
    def _after_validate(state: GraphState) -> Literal["grade", "fail"]:
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
            grading, invocation = self._invoke_agent(
                "grade",
                "grading",
                state["submission"].request_payload(workflow_id, attempt),
                GradingResponse,
                workflow_id,
                attempt,
                state["submission"],
                requires_confidence=True,
            )
        except AgentCallError as error:
            return {"error": f"grading failed: {redact_for_trace(error)}", "grading_passes": attempt}
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
        return {"grading": grading, "review": {}, "grading_passes": attempt}

    @staticmethod
    def _after_grade(state: GraphState) -> Literal["review", "fail"]:
        return "fail" if state.get("error") else "review"

    def _review(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        if state.get("review"):
            return {"review": state["review"]}
        payload = state["submission"].request_payload(workflow_id, state["grading_passes"], state["grading"])
        payload["grading"] = state["grading"]
        try:
            review, invocation = self._invoke_agent(
                "review", "review", payload, ReviewResponse, workflow_id, state["grading_passes"], state["submission"], requires_confidence=True
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

    def _after_review(self, state: GraphState) -> Literal["grade", "suggest", "fail"]:
        if state.get("error"):
            return "fail"
        decision = ReviewDecision(state["review"]["decision"])
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
        if decision is ReviewDecision.REGRADE and state["grading_passes"] <= self.settings.max_regrades:
            return "grade"
        return "fail"

    def _suggest(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        if state.get("suggestions"):
            return {"suggestions": state["suggestions"]}
        if state["submission"].composition.language == "CHINESE":
            # Chinese grading/review remains available, but this English-only agent
            # must not turn a completed assessment into a workflow failure.
            suggestions = SuggestionResponse(
                suggestionStatus=SuggestionStatus.NOT_AVAILABLE_FOR_LANGUAGE,
                suggestions=[],
            ).model_dump(by_alias=True, mode="json")
            self._update(
                workflow_id,
                status=WorkflowStatus.SUGGESTING,
                log_step="SUGGESTION_SKIPPED_LANGUAGE",
                attempt=state["grading_passes"],
                suggestion_output=suggestions,
            )
            return {"suggestions": suggestions}
        payload = state["submission"].request_payload(workflow_id, state["grading_passes"], state["grading"])
        payload.update({"grading": state["grading"], "review": state["review"]})
        try:
            suggestions, invocation = self._invoke_agent(
                "suggest", "suggestion", payload, SuggestionResponse, workflow_id, state["grading_passes"], state["submission"]
            )
        except AgentCallError as error:
            return {"error": f"suggestion generation failed: {redact_for_trace(error)}"}
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
        operation: Literal["grade", "review", "suggest"],
        agent: str,
        payload: dict[str, Any],
        parser: type[BaseModel],
        workflow_id: UUID,
        grading_pass: int,
        submission: WorkflowSubmission,
        *,
        requires_confidence: bool = False,
    ) -> tuple[dict[str, Any], AgentInvocation[Any]]:
        try:
            invocation = getattr(self.agent_client, operation)(payload, parser)
            self._log_agent_attempts(workflow_id, agent, grading_pass, agent_attempt_entries(agent, list(invocation.attempts)))
            result = invocation.response
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
            # A rejected assessment still consumed provider tokens.
            if requires_confidence and result.confidence < self.settings.minimum_confidence:
                raise AgentCallError(agent, f"{agent} confidence is below the configured threshold")
            response = result.model_dump(by_alias=True, mode="json")
            record_agent_call(agent, True)
            return response, invocation
        except AgentCallError as error:
            record_agent_call(agent, False)
            self._log_agent_attempts(workflow_id, agent, grading_pass, agent_attempt_entries(agent, list(error.attempts)))
            raise
        except ValidationError as error:
            record_agent_call(agent, False)
            raise AgentCallError(agent, f"{agent} returned an invalid response") from error

    @staticmethod
    def _report_progress(state: GraphState, status: WorkflowStatus) -> None:
        reporter = state.get("progress_reporter")
        event = progress_event(state["workflow_id"], state["submission"], status)
        if reporter is not None and event is not None:
            reporter(event)

    def _update(self, workflow_id: UUID, **fields: Any) -> WorkflowRecord:
        """Apply a transition only while this runner still holds its fencing lease."""
        if getattr(self, "_active_claim", None) is None:
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
        if getattr(self, "_active_claim", None) is None:
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
        return {"claim": self._active_claim} if getattr(self, "_active_claim", None) is not None else {}

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
