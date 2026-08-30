from __future__ import annotations

from uuid import UUID

from app.config import Settings
from app.agent_types import AgentAttempt, AgentInvocation
from app.models import GradingResponse, ReviewResponse, SuggestionResponse, WorkflowRecord, WorkflowStatus, WorkflowSubmission
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
        return None


class FakeAgents:
    def __init__(self, reviews: list[str]):
        self.reviews = reviews
        self.grade_calls = 0

    @staticmethod
    def _invocation(response):
        return AgentInvocation(response, (AgentAttempt(1, "SUCCEEDED", 0),), "fake-task", "fake-workflow", "fake")

    def ensure_ready(self):
        return None

    def grade(self, payload, parser):
        self.grade_calls += 1
        return self._invocation(GradingResponse.model_validate(
            {
                "overallScore": 72,
                "deepScores": {"Description": 72, "Emotion": 70},
                "criteria": [{"criterion": "Description", "score": 72, "rationale": "Clear detail", "evidence": [{"excerpt": "blue sea"}]}],
                "confidence": 0.9,
            }
        ))

    def review(self, payload, parser):
        return self._invocation(ReviewResponse.model_validate({"decision": self.reviews.pop(0), "confidence": 0.9, "findings": []}))

    def suggest(self, payload, parser):
        return self._invocation(SuggestionResponse.model_validate(
            {"suggestions": [{"criterion": "Emotion", "priority": "HIGH", "recommendation": "Explain how you felt.", "learningTip": "Use a feeling and a reason."}]}
        ))


def workflow_record() -> WorkflowRecord:
    submission = WorkflowSubmission.from_kafka_payload(
        {"compositionId": 3, "userId": 5, "title": "A day out", "content": "I saw the blue sea.", "language": "ENGLISH"}
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
    assert "Improvement suggestions" in completed.completion_event["feedback"]
    assert completed.final_report["review"]["decision"] == "APPROVE"


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
        {"suggestions": [{"criterion": "Placeholder", "priority": "HIGH", "recommendation": "Development only."}]}
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
    assert completed.grading_passes == 2


def test_rejected_review_returns_safe_retry_notice_without_score():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)

    failed = OrchestrationWorkflow(settings(), repository, FakeAgents(["REJECT"])).run(record)

    assert failed.status is WorkflowStatus.FAILED
    assert failed.completion_event["success"] is False
    assert failed.completion_event["score"] is None
    assert failed.completion_event["failureReason"] == SAFE_FAILURE_MESSAGE


def test_terminal_duplicate_does_not_run_agents_again():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)
    agents = FakeAgents(["APPROVE"])
    workflow = OrchestrationWorkflow(settings(), repository, agents)

    workflow.run(record)
    workflow.run(repository.get(record.workflow_id))

    assert agents.grade_calls == 1


def test_approved_assessment_reports_only_safe_student_progress_stages():
    repository = MemoryRepository()
    record = workflow_record()
    repository.add(record)
    events: list[dict] = []

    OrchestrationWorkflow(settings(), repository, FakeAgents(["APPROVE"]), progress_reporter=events.append).run(record)

    assert [(event["stage"], event["message"]) for event in events] == [
        ("GRADING", "Assessing your composition…"),
        ("REVIEWING", "Checking the assessment for accuracy…"),
        ("SUGGESTING", "Creating personalised improvement suggestions…"),
    ]
