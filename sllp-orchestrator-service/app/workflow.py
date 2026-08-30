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
from app.models import GradingResponse, ReviewDecision, ReviewResponse, SuggestionResponse, WorkflowRecord, WorkflowStatus, WorkflowSubmission
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

    def run(self, record: WorkflowRecord, progress_reporter: ProgressReporter | None = None) -> WorkflowRecord:
        if record.status in {WorkflowStatus.COMPLETED, WorkflowStatus.FAILED}:
            return record
        state: GraphState = {
            "workflow_id": record.workflow_id,
            "submission": WorkflowSubmission.model_validate(record.input_payload),
            "grading_passes": record.grading_passes,
            "progress_reporter": progress_reporter or self.progress_reporter,
        }
        self.graph.invoke(state)
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
        graph.add_edge(START, "validate")
        graph.add_conditional_edges("validate", self._after_validate, {"grade": "grade", "fail": "fail"})
        graph.add_conditional_edges("grade", self._after_grade, {"review": "review", "fail": "fail"})
        graph.add_conditional_edges("review", self._after_review, {"grade": "grade", "suggest": "suggest", "fail": "fail"})
        graph.add_conditional_edges("suggest", self._after_suggest, {"assemble": "assemble", "fail": "fail"})
        graph.add_edge("assemble", END)
        graph.add_edge("fail", END)
        return graph.compile()

    def _validate(self, state: GraphState) -> GraphState:
        error = validate_submission(state["submission"], self.settings)
        if error:
            return {"error": error}
        try:
            provider_details = self.agent_client.ensure_ready()
        except Exception as provider_error:
            return {"error": f"assessment provider is unavailable: {redact_for_trace(provider_error)}"}
        self.repository.update(
            state["workflow_id"],
            status=WorkflowStatus.GRADING,
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
        self.repository.update(
            workflow_id,
            status=status,
            log_step="GRADING_STARTED",
            attempt=attempt,
            grading_passes=attempt,
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
                requires_confidence=True,
            )
        except AgentCallError as error:
            return {"error": f"grading failed: {redact_for_trace(error)}", "grading_passes": attempt}
        self.repository.update(
            workflow_id,
            status=WorkflowStatus.REVIEWING,
            log_step="GRADING_COMPLETED",
            attempt=attempt,
            grading_output=grading,
            log_details=response_metadata(grading, invocation),
        )
        self._report_progress(state, WorkflowStatus.REVIEWING)
        return {"grading": grading, "grading_passes": attempt}

    @staticmethod
    def _after_grade(state: GraphState) -> Literal["review", "fail"]:
        return "fail" if state.get("error") else "review"

    def _review(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        payload = state["submission"].request_payload(workflow_id, state["grading_passes"], state["grading"])
        payload["grading"] = state["grading"]
        try:
            review, invocation = self._invoke_agent(
                "review", "review", payload, ReviewResponse, workflow_id, state["grading_passes"], requires_confidence=True
            )
        except AgentCallError as error:
            return {"error": f"review failed: {redact_for_trace(error)}"}
        self.repository.update(
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
            self.repository.update(
                state["workflow_id"],
                status=WorkflowStatus.SUGGESTING,
                log_step="REVIEW_APPROVED",
                attempt=state["grading_passes"],
            )
            self._report_progress(state, WorkflowStatus.SUGGESTING)
            return "suggest"
        if decision is ReviewDecision.REGRADE and state["grading_passes"] <= self.settings.max_regrades:
            return "grade"
        return "fail"

    def _suggest(self, state: GraphState) -> GraphState:
        workflow_id = state["workflow_id"]
        payload = state["submission"].request_payload(workflow_id, state["grading_passes"], state["grading"])
        payload.update({"grading": state["grading"], "review": state["review"]})
        try:
            suggestions, invocation = self._invoke_agent(
                "suggest", "suggestion", payload, SuggestionResponse, workflow_id, state["grading_passes"]
            )
        except AgentCallError as error:
            return {"error": f"suggestion generation failed: {redact_for_trace(error)}"}
        self.repository.update(
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
        self.repository.update(
            state["workflow_id"],
            status=WorkflowStatus.COMPLETED,
            log_step="ASSEMBLED",
            final_report=report,
            completion_event=completion,
        )
        return {"completion_event": completion}

    def _fail(self, state: GraphState) -> GraphState:
        completion = failed_assessment(state["submission"])
        reason = state.get("error", "review did not approve the assessment")
        self.repository.record_failure(state["workflow_id"], {"message": redact_for_trace(reason)}, completion)
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
        *,
        requires_confidence: bool = False,
    ) -> tuple[dict[str, Any], AgentInvocation[Any]]:
        try:
            invocation = getattr(self.agent_client, operation)(payload, parser)
            self.repository.log_agent_attempts(
                workflow_id, agent, grading_pass, agent_attempt_entries(agent, list(invocation.attempts))
            )
            result = invocation.response
            if requires_confidence and result.confidence < self.settings.minimum_confidence:
                raise AgentCallError(agent, f"{agent} confidence is below the configured threshold")
            response = result.model_dump(by_alias=True, mode="json")
            return response, invocation
        except AgentCallError as error:
            self.repository.log_agent_attempts(workflow_id, agent, grading_pass, agent_attempt_entries(agent, list(error.attempts)))
            raise
        except ValidationError as error:
            raise AgentCallError(agent, f"{agent} returned an invalid response") from error

    @staticmethod
    def _report_progress(state: GraphState, status: WorkflowStatus) -> None:
        reporter = state.get("progress_reporter")
        event = progress_event(state["workflow_id"], state["submission"], status)
        if reporter is not None and event is not None:
            reporter(event)
