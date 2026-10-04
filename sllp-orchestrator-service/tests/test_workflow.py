from __future__ import annotations

from dataclasses import replace
from uuid import UUID

from app.config import Settings
from app.agent_types import AgentAttempt, AgentCallError, AgentInvocation
from app.models import EvidenceResponse, GradingResponse, ReviewResponse, SuggestionResponse, WorkflowRecord, WorkflowStatus, WorkflowSubmission
from app.assessment_report import render_feedback
from app.workflow import OrchestrationWorkflow, SAFE_FAILURE_MESSAGE


def settings() -> Settings:
    return Settings(
        database_url="unused",
        kafka_bootstrap_servers="unused",
        kafka_group_id="unused",
        grading_requested_topic="requested",
        grading_completed_topic="completed",
        grading_progress_topic="progress",
        http_timeout_seconds=1,
        max_agent_retries=2,
        max_regrades=1,
        minimum_confidence=0.6,
        max_composition_characters=20_000,
        kafka_enabled=False,
        agent_card_discovery_enabled=False,
        agent_card_refresh_seconds=300,
        agent_card_max_stale_seconds=3600,
        grading_agent_card_url=None,
        review_agent_card_url=None,
        suggestion_agent_card_url=None,
    )


class MemoryRepository:
    def __init__(self):
        self.records: dict[UUID, WorkflowRecord] = {}
        self.agent_usage_events: list[dict] = []
        self.agent_attempt_logs: list[tuple] = []
        self.cost_micros = 0

    def add(self, record: WorkflowRecord) -> None:
        self.records[record.workflow_id] = record

    def get(self, workflow_id: UUID) -> WorkflowRecord:
        return self.records[workflow_id]

    def update(self, workflow_id: UUID, *, status=None, log_step=None, attempt=0, log_details=None, latency_ms=None, **fields):
        record = self.records[workflow_id]
        if status is not None:
            record.status = status
        for key, value in fields.items():
            if value is not None:
                setattr(record, key, value)
        return record

    def record_failure(self, workflow_id, error_details, completion_event):
        return self.update(workflow_id, status=WorkflowStatus.FAILED, error_details=error_details, completion_event=completion_event)

    def log_agent_attempts(self, workflow_id, agent, attempt, entries):
        self.agent_attempt_logs.append((workflow_id, agent, attempt, entries))

    def record_agent_usage(self, **kwargs):
        self.agent_usage_events.append(kwargs)

    def workflow_cost_micros(self, workflow_id):
        return self.cost_micros


def usage():
    return {
        "provider": "test-provider",
        "model": "test-model",
        "promptTokens": 100,
        "completionTokens": 20,
        "pricingVersion": "catalog-v1",
        "inputPriceMicrosPerMillion": 1_000_000,
        "cachedInputPriceMicrosPerMillion": 500_000,
        "outputPriceMicrosPerMillion": 2_000_000,
    }


class FakeAgents:
    def __init__(self, reviews: list[str]):
        self.reviews = reviews
        self.evidence_calls = 0
        self.grade_calls = 0
        self.suggest_calls = 0

    @staticmethod
    def _invocation(response):
        return AgentInvocation(response, (AgentAttempt(1, "SUCCEEDED", 0),), "fake-task", "fake-workflow", "fake")

    def ensure_ready(self, include_suggestions=True):
        return None

    def grade(self, payload, parser):
        self.grade_calls += 1
        return self._invocation(GradingResponse.model_validate(
            {
                "overallScore": 72,
                "rubricScores": {"Description": 72, "Emotion": 70},
                "criteria": [
                    {"criterion": "Description", "score": 72, "rationale": "Clear detail", "evidence": [{"excerpt": "blue sea"}]},
                    {"criterion": "Emotion", "score": 70, "rationale": "No feeling is stated", "evidence": [{"excerpt": "blue sea"}]},
                ],
                "confidence": 0.9,
                "usage": usage(),
            }
        ))

    def evidence(self, payload, parser):
        self.evidence_calls += 1
        return self._invocation(EvidenceResponse.model_validate({
            "plan": ["Find rubric evidence."], "rubricVersion": "test-rubric-v1",
            "rubricSnapshot": {"version": "test-rubric-v1", "criteria": [{"name": "Description", "weight": 100, "guidance": "Use clear details."}]},
            "criteria": [{"criterion": "Description", "evidence": [{"excerpt": "blue sea", "polarity": "STRENGTH", "note": "specific detail", "verifiedOnFirstPass": True}]}],
            "usage": usage(),
        }))

    def review(self, payload, parser):
        return self._invocation(ReviewResponse.model_validate({"decision": self.reviews.pop(0), "confidence": 0.9, "findings": [], "usage": usage()}))

    def suggest(self, payload, parser):
        self.suggest_calls += 1
        return self._invocation(SuggestionResponse.model_validate(
            {"suggestions": [{"criterion": "Emotion", "priority": "HIGH", "recommendation": "Explain how you felt.", "exampleRevision": "I felt calm as I watched the blue sea.", "learningTip": "Use a feeling and a reason.", "supportingEvidence": {"excerpt": "blue sea"}}], "usage": usage()}
        ))


class FailingGradeAgents:
    def ensure_ready(self, include_suggestions=True):
        return None

    def grade(self, payload, parser):
        raise AgentCallError("grading", "provider timeout", (AgentAttempt(1, "FAILED", 42, 504, "timeout"),))

    def evidence(self, payload, parser):
        return AgentInvocation(EvidenceResponse.model_validate({
            "rubricVersion": "test-rubric-v1",
            "rubricSnapshot": {"version": "test-rubric-v1", "criteria": [{"name": "Description", "weight": 100, "guidance": "Use clear details."}]},
            "criteria": [{"criterion": "Description", "evidence": [{"excerpt": "blue sea", "polarity": "STRENGTH", "note": "detail", "verifiedOnFirstPass": True}]}],
            "usage": usage(),
        }), (AgentAttempt(1, "SUCCEEDED", 0),), "fake-evidence", payload["workflowId"], "fake")


class FailingSuggestionAgents(FakeAgents):
    def suggest(self, payload, parser):
        self.suggest_calls += 1
        raise AgentCallError("suggestion", "provider timeout", (AgentAttempt(1, "FAILED", 42, 504, "timeout"),))


def workflow_record(language: str = "ENGLISH") -> WorkflowRecord:
    submission = WorkflowSubmission.from_kafka_payload(
        {
            "requestId": "9b6efdf7-9ed7-4dcc-b1e5-d617b9c644a0",
            "compositionId": 3,
            "userId": 5,
            "title": "A day out",
            "content": "I saw the blue sea.",
            "language": language,
            "requestedAt": "2026-08-29T09:00:00Z",
        }
    )
    return WorkflowRecord(source_event_id="topic:0:1", input_payload=submission.model_dump(by_alias=True, mode="json"))


def test_approved_assessment_publishes_structured_report_and_compatible_feedback():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)

    completed = OrchestrationWorkflow(settings(), repository, FakeAgents(["APPROVE"])).run(record)

    assert completed.status is WorkflowStatus.COMPLETED
    assert completed.completion_event["success"] is True
    assert completed.completion_event["score"] == 72
    assert completed.completion_event["rubricScores"] == {"Description": 72, "Emotion": 70}
    suggestion = completed.completion_event["suggestions"][0]
    assert suggestion["supportingEvidence"]["excerpt"] == "blue sea"
    assert suggestion["learningTip"] == "Use a feeling and a reason."
    assert "supporting_evidence" not in suggestion
    assert completed.completion_event["requestId"] == "9b6efdf7-9ed7-4dcc-b1e5-d617b9c644a0"
    assert completed.completion_event["requestedAt"] == "2026-08-29T09:00:00"
    assert "Improvement suggestions" in completed.completion_event["feedback"]
    assert completed.final_report["review"]["decision"] == "APPROVE"
    assert [event["agent"] for event in repository.agent_usage_events] == ["evidence", "grading", "review", "suggestion"]
    assert {event["request_id"] for event in repository.agent_usage_events} == {
        UUID("9b6efdf7-9ed7-4dcc-b1e5-d617b9c644a0")
    }


def test_suggestion_failure_preserves_the_approved_assessment():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)

    completed = OrchestrationWorkflow(settings(), repository, FailingSuggestionAgents(["APPROVE"])).run(record)

    assert completed.status is WorkflowStatus.COMPLETED
    assert completed.completion_event["success"] is True
    assert completed.completion_event["score"] == 72
    assert completed.completion_event["suggestionStatus"] == "TEMPORARILY_UNAVAILABLE"
    assert completed.completion_event["suggestions"] == []
    assert "Overall score: 72/100" in completed.completion_event["feedback"]


def test_placeholder_assessment_is_visibly_labelled_in_feedback():
    grading = GradingResponse.model_validate(
        {
            "overallScore": 75,
            "criteria": [{"criterion": "Placeholder", "score": 75, "rationale": "Development only", "evidence": [{"excerpt": "Submitted composition"}]}],
            "confidence": 0.99,
            "modelVersion": "placeholder-v1",
        }
    )
    suggestions = SuggestionResponse.model_validate(
        {"suggestions": [{"criterion": "Placeholder", "priority": "HIGH", "recommendation": "Development only.", "exampleRevision": "Use a real assessment result.", "supportingEvidence": {"excerpt": "Submitted composition"}}]}
    )

    assert render_feedback(grading, suggestions).startswith("Development placeholder assessment")


def test_review_can_request_exactly_one_regrade_before_approval():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)
    agents = FakeAgents(["REGRADE", "APPROVE"])

    completed = OrchestrationWorkflow(settings(), repository, agents).run(record)

    assert completed.status is WorkflowStatus.COMPLETED
    assert agents.grade_calls == 2
    assert agents.evidence_calls == 2
    assert completed.grading_passes == 2


def test_rejected_review_returns_safe_retry_notice_without_score():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)

    agents = FakeAgents(["REJECT", "REJECT"])
    failed = OrchestrationWorkflow(settings(), repository, agents).run(record)

    assert failed.status is WorkflowStatus.FAILED
    assert agents.grade_calls == 2
    assert agents.evidence_calls == 2
    assert failed.completion_event["success"] is False
    assert failed.completion_event["score"] is None
    assert failed.completion_event["failureReason"] == SAFE_FAILURE_MESSAGE


def test_failed_agent_attempt_is_logged_for_agent_reliability_metrics():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)

    failed = OrchestrationWorkflow(settings(), repository, FailingGradeAgents()).run(record)

    assert failed.status is WorkflowStatus.FAILED
    assert [event["agent"] for event in repository.agent_usage_events] == ["evidence"]
    _, agent, attempt, entries = repository.agent_attempt_logs[-1]
    assert (agent, attempt) == ("grading", 1)
    assert entries == [{"agent": "grading", "agentAttempt": 1, "status": "FAILED", "httpStatus": 504, "error": "timeout", "latencyMs": 42}]


def test_spend_guard_stops_before_the_next_agent_call():
    repository = MemoryRepository()
    repository.cost_micros = 100
    record = workflow_record()
    repository.add(record)
    agents = FakeAgents(["APPROVE"])

    failed = OrchestrationWorkflow(replace(settings(), max_workflow_cost_micros=100), repository, agents).run(record)

    assert failed.status is WorkflowStatus.FAILED
    assert agents.evidence_calls == 0


def test_terminal_duplicate_does_not_run_agents_again():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)
    agents = FakeAgents(["APPROVE"])
    workflow = OrchestrationWorkflow(settings(), repository, agents)

    workflow.run(record)
    workflow.run(repository.get(record.workflow_id))

    assert agents.grade_calls == 1


def test_low_confidence_result_still_records_provider_usage():
    class LowConfidenceAgents(FakeAgents):
        def grade(self, payload, parser):
            invocation = super().grade(payload, parser)
            return self._invocation(invocation.response.model_copy(update={"confidence": 0.1}))

    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)
    completed = OrchestrationWorkflow(settings(), repository, LowConfidenceAgents([])).run(record)
    assert completed.status is WorkflowStatus.FAILED
    assert [event["agent"] for event in repository.agent_usage_events] == ["evidence", "grading", "evidence", "grading"]


def test_a2a_response_without_usage_is_recorded_as_a_contract_failure():
    class MissingUsageAgents(FakeAgents):
        def grade(self, payload, parser):
            invocation = super().grade(payload, parser)
            return AgentInvocation(
                invocation.response.model_copy(update={"usage": None}),
                invocation.attempts,
                invocation.task_id,
                invocation.context_id,
                invocation.provider,
            )

    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)

    failed = OrchestrationWorkflow(replace(settings(), agent_provider="a2a"), repository, MissingUsageAgents([])).run(record)

    assert failed.status is WorkflowStatus.FAILED
    _, agent, _, entries = repository.agent_attempt_logs[-1]
    assert agent == "grading"
    assert [entry["status"] for entry in entries] == ["SUCCEEDED", "MISSING_USAGE"]


def test_recovery_resumes_from_persisted_grading_without_grading_twice():
    repository = MemoryRepository()
    record = workflow_record()
    record.status = WorkflowStatus.REVIEWING
    record.grading_passes = 1
    record.evidence_output = {
        "rubricVersion": "test-rubric-v1",
        "rubricSnapshot": {"version": "test-rubric-v1", "criteria": [{"name": "Description", "weight": 100, "guidance": "Use clear details."}]},
    }
    record.grading_output = GradingResponse.model_validate(
        {
            "overallScore": 72,
            "criteria": [{"criterion": "Description", "score": 72, "rationale": "Clear detail", "evidence": [{"excerpt": "blue sea"}]}],
            "confidence": 0.9,
        }
    ).model_dump(by_alias=True, mode="json")
    repository.add(record)
    agents = FakeAgents(["APPROVE"])

    completed = OrchestrationWorkflow(settings(), repository, agents).run(record)

    assert completed.status is WorkflowStatus.COMPLETED
    assert agents.grade_calls == 0


def test_approved_assessment_reports_only_safe_student_progress_stages():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)
    events: list[dict] = []

    OrchestrationWorkflow(settings(), repository, FakeAgents(["APPROVE"]), progress_reporter=events.append).run(record)

    assert [(event["stage"], event["message"]) for event in events] == [
        ("EVIDENCING", "Gathering supporting passages from your composition…"),
        ("GRADING", "Assessing your composition…"),
        ("REVIEWING", "Checking the assessment for accuracy…"),
        ("SUGGESTING", "Creating personalised improvement suggestions…"),
    ]


def test_chinese_assessment_completes_without_calling_the_english_suggestions_agent():
    repository = MemoryRepository()
    record = workflow_record("CHINESE")
    repository.add(record)
    agents = FakeAgents(["APPROVE"])

    completed = OrchestrationWorkflow(settings(), repository, agents).run(record)

    assert completed.status is WorkflowStatus.COMPLETED
    assert agents.suggest_calls == 0
    assert completed.final_report["suggestions"]["suggestionStatus"] == "NOT_AVAILABLE_FOR_LANGUAGE"
    assert completed.completion_event["suggestionStatus"] == "NOT_AVAILABLE_FOR_LANGUAGE"
    assert completed.completion_event["suggestions"] == []
