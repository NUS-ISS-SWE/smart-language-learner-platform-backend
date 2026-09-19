import json
from dataclasses import replace
from types import SimpleNamespace

import httpx
import pytest

from app.agent_client import AgentCallError, AgentClient
from app.config import Settings
from app.models import GradingResponse, ReviewResponse, SuggestionResponse


def settings() -> Settings:
    return Settings(
        database_url="postgresql+psycopg://unused",
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
        agent_provider="a2a",
    )


VALID_GRADING = {
    "overallScore": 70,
    "deepScores": {"Description": 70},
    "criteria": [{"criterion": "Description", "score": 70, "rationale": "Clear setting", "evidence": [{"excerpt": "at the beach"}]}],
    "confidence": 0.8,
}


class FakeRegistry:
    def get(self, key: str):
        return SimpleNamespace(service_url=f"http://{key}.test/a2a")


def a2a_completed(artifact: dict) -> dict:
    return {
        "jsonrpc": "2.0",
        "id": "response-id",
        "result": {
            "task": {
                "id": "task-123",
                "contextId": "workflow-123",
                "status": {"state": "TASK_STATE_COMPLETED"},
                "artifacts": [{"artifactId": "result", "parts": [{"data": artifact, "mediaType": "application/json"}]}],
            }
        },
    }


def test_retries_transient_agent_failure_then_returns_valid_contract():
    calls = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal calls
        calls += 1
        return httpx.Response(503 if calls == 1 else 200, json={} if calls == 1 else a2a_completed(VALID_GRADING))

    client = AgentClient(settings(), FakeRegistry(), httpx.Client(transport=httpx.MockTransport(handler)), sleeper=lambda _: None)
    result = client.grade({"workflowId": "workflow-123"}, GradingResponse)

    assert result.response.overall_score == 70
    assert result.task_id == "task-123"
    assert result.context_id == "workflow-123"
    assert calls == 2


def test_rejects_malformed_agent_contract_without_retrying():
    client = AgentClient(
        settings(),
        FakeRegistry(),
        httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json=a2a_completed({"score": 70})))),
        sleeper=lambda _: None,
    )

    with pytest.raises(AgentCallError, match="invalid response"):
        client.grade({"workflowId": "workflow-123"}, GradingResponse)


def test_sends_a2a_send_message_with_workflow_context_and_json_part():
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["json"] = json.loads(request.content)
        captured["headers"] = request.headers
        return httpx.Response(200, json=a2a_completed(VALID_GRADING))

    client = AgentClient(settings(), FakeRegistry(), httpx.Client(transport=httpx.MockTransport(handler)), sleeper=lambda _: None)
    client.grade({"workflowId": "workflow-123", "schemaVersion": "1.0"}, GradingResponse)

    assert captured["json"]["jsonrpc"] == "2.0"
    assert captured["json"]["method"] == "SendMessage"
    assert captured["json"]["params"]["message"]["contextId"] == "workflow-123"
    assert captured["json"]["params"]["message"]["parts"][0]["data"]["schemaVersion"] == "1.0"
    assert captured["headers"]["a2a-version"] == "1.0"


def test_rejects_a2a_response_without_a_completed_task_artifact():
    working_task = {"jsonrpc": "2.0", "result": {"task": {"id": "task-123", "status": {"state": "TASK_STATE_WORKING"}}}}
    client = AgentClient(
        settings(),
        FakeRegistry(),
        httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json=working_task))),
        sleeper=lambda _: None,
    )

    with pytest.raises(AgentCallError, match="non-terminal A2A task state"):
        client.grade({"workflowId": "workflow-123"}, GradingResponse)


def test_placeholder_provider_runs_the_full_contract_without_http_calls():
    client = AgentClient(
        replace(settings(), agent_provider="placeholder"),
        client=httpx.Client(transport=httpx.MockTransport(lambda request: pytest.fail("placeholder must not call HTTP"))),
    )

    grading = client.grade({"workflowId": "workflow-123"}, GradingResponse)
    review = client.review({"workflowId": "workflow-123"}, ReviewResponse)
    suggestions = client.suggest({"workflowId": "workflow-123"}, SuggestionResponse)

    assert client.ensure_ready()["mode"] == "development-placeholder"
    assert client.client is None
    assert grading.provider == "placeholder"
    assert grading.response.model_version == "placeholder-v1"
    assert grading.response.usage is not None
    assert grading.response.usage.total_tokens == 0
    assert review.response.decision == "APPROVE"
    assert suggestions.response.suggestions[0].criterion == "Development placeholder"
