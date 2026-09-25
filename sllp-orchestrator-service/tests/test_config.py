import pytest

from app.config import Settings


@pytest.fixture(autouse=True)
def clear_a2a_environment(monkeypatch):
    for name in (
        "AGENT_PROVIDER",
        "EVIDENCE_AGENT_TOKEN",
        "GRADING_AGENT_TOKEN",
        "REVIEW_AGENT_TOKEN",
        "SUGGESTION_AGENT_TOKEN",
    ):
        monkeypatch.delenv(name, raising=False)


def test_a2a_is_the_default_and_requires_all_peer_tokens():
    with pytest.raises(ValueError, match="EVIDENCE_AGENT_TOKEN"):
        Settings.from_environment()


def test_placeholder_provider_allows_tokenless_local_workflow(monkeypatch):
    monkeypatch.setenv("AGENT_PROVIDER", "placeholder")

    settings = Settings.from_environment()

    assert settings.agent_provider == "placeholder"


def test_a2a_provider_accepts_all_configured_peer_tokens(monkeypatch):
    for name in (
        "EVIDENCE_AGENT_TOKEN",
        "GRADING_AGENT_TOKEN",
        "REVIEW_AGENT_TOKEN",
        "SUGGESTION_AGENT_TOKEN",
    ):
        monkeypatch.setenv(name, "test-token")

    settings = Settings.from_environment()

    assert settings.agent_provider == "a2a"


def test_unknown_provider_is_rejected(monkeypatch):
    monkeypatch.setenv("AGENT_PROVIDER", "unsupported")

    with pytest.raises(ValueError, match="AGENT_PROVIDER"):
        Settings.from_environment()
