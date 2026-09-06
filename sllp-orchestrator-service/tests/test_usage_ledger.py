from contextlib import contextmanager
from uuid import UUID

from sqlalchemy.dialects import postgresql

from app.models import AgentUsage
from app.repository import WorkflowRepository


WORKFLOW_ID = UUID("9b6efdf7-9ed7-4dcc-b1e5-d617b9c644a0")
REQUEST_ID = UUID("b0b2f06d-7b1c-4c80-8b1c-ff8b45f39753")


def usage() -> AgentUsage:
    return AgentUsage.model_validate(
        {
            "provider": "provider",
            "model": "model",
            "promptTokens": 1_002,
            "cachedInputTokens": 2,
            "completionTokens": 3,
            "pricingVersion": "catalog-v1",
            "inputPriceMicrosPerMillion": 1_000_000,
            "cachedInputPriceMicrosPerMillion": 500_000,
            "outputPriceMicrosPerMillion": 2_000_000,
        }
    )


def test_agent_usage_values_calculate_cost_from_each_provider_token_category():
    values = WorkflowRepository.agent_usage_values(
        workflow_id=WORKFLOW_ID,
        request_id=REQUEST_ID,
        user_id=5,
        composition_id=3,
        agent="grading",
        attempt=1,
        usage=usage(),
    )

    # 1,000 non-cached prompt tokens + 2 cached + 3 output tokens.
    assert values["total_tokens"] == 1_005
    assert values["estimated_cost_micros"] == 1_007
    assert values["event_id"] == WorkflowRepository.agent_usage_values(
        workflow_id=WORKFLOW_ID, request_id=REQUEST_ID, user_id=5, composition_id=3,
        agent="grading", attempt=1, usage=usage(),
    )["event_id"]
    assert WorkflowRepository._cost(1, 499_999) == 0
    assert WorkflowRepository._cost(1, 500_000) == 1


class CapturingConnection:
    def __init__(self) -> None:
        self.statements = []

    def execute(self, statement):
        self.statements.append(statement)


class CapturingEngine:
    def __init__(self) -> None:
        self.connection = CapturingConnection()

    @contextmanager
    def begin(self):
        yield self.connection


def test_agent_usage_insert_has_a_stable_event_id_and_database_conflict_guard():
    engine = CapturingEngine()
    repository = WorkflowRepository("unused", engine=engine)
    arguments = dict(workflow_id=WORKFLOW_ID, request_id=REQUEST_ID, user_id=5, composition_id=3,
                     agent="grading", attempt=1, usage=usage())

    repository.record_agent_usage(**arguments)
    repository.record_agent_usage(**arguments)

    assert len(engine.connection.statements) == 2
    compiled = [statement.compile(dialect=postgresql.dialect()) for statement in engine.connection.statements]
    assert all("ON CONFLICT (event_id) DO NOTHING" in str(statement) for statement in compiled)
    assert compiled[0].params["event_id"] == compiled[1].params["event_id"]


def test_recovery_counts_a_new_provider_task_but_deduplicates_the_same_task():
    arguments = dict(workflow_id=WORKFLOW_ID, request_id=REQUEST_ID, user_id=5, composition_id=3,
                     agent="grading", attempt=1, usage=usage())
    first = WorkflowRepository.agent_usage_values(**arguments, invocation_id="provider-task-1")
    replay = WorkflowRepository.agent_usage_values(**arguments, invocation_id="provider-task-1")
    recovered = WorkflowRepository.agent_usage_values(**arguments, invocation_id="provider-task-2")
    assert first["event_id"] == replay["event_id"]
    assert first["event_id"] != recovered["event_id"]
