import httpx
import pytest

from app.agent_registry import AgentDiscoveryError, AgentRegistry
from app.config import Settings


def settings(card_url: str = "http://grading.test/.well-known/agent-card.json") -> Settings:
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
        agent_card_discovery_enabled=True,
        agent_card_refresh_seconds=300,
        agent_card_max_stale_seconds=3600,
        grading_agent_card_url=card_url,
        review_agent_card_url="http://review.test/.well-known/agent-card.json",
        suggestion_agent_card_url="http://suggestion.test/.well-known/agent-card.json",
        evidence_agent_card_url="http://evidence.test/.well-known/agent-card.json",
    )


def card(name: str, url: str, skill: str) -> dict:
    origin = url.removesuffix("/a2a")
    return {
        "name": name,
        "url": url,
        "protocolVersion": "1.0",
        "skills": [{"id": skill, "name": skill}],
        "sllp": {
            "contractVersion": "2.0",
            "inputSchemaUrl": f"{origin}/.well-known/sllp-contracts/{skill}-v2-input.json",
            "outputSchemaUrl": f"{origin}/.well-known/sllp-contracts/{skill}-v2-output.json",
        },
    }


def test_discovers_and_validates_allowlisted_agent_cards():
    def handler(request: httpx.Request) -> httpx.Response:
        if "/.well-known/sllp-contracts/" in request.url.path:
            return httpx.Response(200, json={
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
            })
        host = request.url.host
        if host == "evidence.test":
            payload = card("Evidence", "http://evidence.test/a2a", "evidence-collection")
        elif host == "grading.test":
            payload = card("Grading", "http://grading.test/a2a", "composition-grading")
        elif host == "review.test":
            payload = card("Review", "http://review.test/a2a", "assessment-review")
        else:
            payload = card("Suggestion", "http://suggestion.test/a2a", "writing-suggestions")
        return httpx.Response(200, json=payload)

    registry = AgentRegistry(settings(), httpx.Client(transport=httpx.MockTransport(handler)))
    cards = registry.ensure_ready()

    assert cards["grading"]["skill"] == "composition-grading"
    assert cards["review"]["contractVersion"] == "2.0"
    assert cards["evidence"]["inputSchemaUrl"].endswith("evidence-collection-v2-input.json")


def test_rejects_a_card_that_advertises_the_wrong_skill():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=card("Wrong", "http://grading.test/a2a", "unrelated-skill"))

    registry = AgentRegistry(settings(), httpx.Client(transport=httpx.MockTransport(handler)))

    with pytest.raises(AgentDiscoveryError):
        registry.ensure_ready()


def test_rejects_a_card_without_a2a_v1_protocol_version():
    def handler(request: httpx.Request) -> httpx.Response:
        payload = card("Grading", "http://grading.test/a2a", "composition-grading")
        payload.pop("protocolVersion")
        return httpx.Response(200, json=payload)

    registry = AgentRegistry(settings(), httpx.Client(transport=httpx.MockTransport(handler)))

    with pytest.raises(AgentDiscoveryError):
        registry.ensure_ready()


def test_chinese_workflow_does_not_require_the_english_suggestion_peer():
    def handler(request: httpx.Request) -> httpx.Response:
        if "/.well-known/sllp-contracts/" in request.url.path:
            return httpx.Response(200, json={
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
            })
        if request.url.host == "evidence.test":
            return httpx.Response(200, json=card("Evidence", "http://evidence.test/a2a", "evidence-collection"))
        if request.url.host == "grading.test":
            return httpx.Response(200, json=card("Grading", "http://grading.test/a2a", "composition-grading"))
        if request.url.host == "review.test":
            return httpx.Response(200, json=card("Review", "http://review.test/a2a", "assessment-review"))
        return httpx.Response(503)

    registry = AgentRegistry(settings(), httpx.Client(transport=httpx.MockTransport(handler)))
    assert set(registry.ensure_ready(include_suggestions=False)) == {"evidence", "grading", "review"}
    assert registry.get("grading").service_url == "http://grading.test/a2a"
    with pytest.raises(AgentDiscoveryError):
        registry.ensure_ready()
