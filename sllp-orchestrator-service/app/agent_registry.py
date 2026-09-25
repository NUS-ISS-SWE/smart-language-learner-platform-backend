from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from typing import Any
from urllib.parse import urlparse

import httpx
from pydantic import BaseModel, ConfigDict, Field

from app.config import Settings

LOGGER = logging.getLogger(__name__)


class AgentDiscoveryError(RuntimeError):
    pass


class AgentSkill(BaseModel):
    model_config = ConfigDict(extra="allow")

    id: str
    name: str | None = None
    description: str | None = None


class AgentCard(BaseModel):
    """A2A Agent Card fields required by the SLLP capability-card profile."""

    model_config = ConfigDict(extra="allow")

    name: str
    url: str
    protocol_version: str | None = Field(default=None, alias="protocolVersion")
    skills: list[AgentSkill] = Field(default_factory=list)
    version: str | None = None
    sllp: dict[str, Any] | None = None


@dataclass(frozen=True)
class PeerAgent:
    key: str
    required_skill: str
    card_url: str | None


@dataclass(frozen=True)
class VerifiedAgentCard:
    key: str
    name: str
    card_url: str
    service_url: str
    skill: str
    contract_version: str
    verified_at_monotonic: float

    def trace_details(self) -> dict[str, str]:
        return {
            "agent": self.key,
            "agentName": self.name,
            "agentCardUrl": self.card_url,
            "agentServiceUrl": self.service_url,
            "skill": self.skill,
            "contractVersion": self.contract_version,
        }


class AgentRegistry:
    """Verifies allowlisted peer Agent Cards before delegating an assessment.

    Agent Card `url` values are A2A JSON-RPC endpoints. The orchestrator uses
    them directly for `SendMessage`; it does not retain a parallel custom agent
    HTTP API.
    """

    def __init__(self, settings: Settings, client: httpx.Client | None = None):
        self.settings = settings
        self.client = client or httpx.Client(timeout=settings.http_timeout_seconds)
        self.peers = (
            PeerAgent("evidence", "evidence-collection", settings.evidence_agent_card_url),
            PeerAgent("grading", "composition-grading", settings.grading_agent_card_url),
            PeerAgent("review", "assessment-review", settings.review_agent_card_url),
            PeerAgent("suggestion", "writing-suggestions", settings.suggestion_agent_card_url),
        )
        self._cache: dict[str, VerifiedAgentCard] = {}

    def close(self) -> None:
        self.client.close()

    def ensure_ready(self, include_suggestions: bool = True) -> dict[str, dict[str, str]]:
        if not self.settings.agent_card_discovery_enabled:
            raise AgentDiscoveryError("agent card discovery is disabled; A2A delegation requires verified cards")
        verified: dict[str, dict[str, str]] = {}
        for peer in self.peers:
            if peer.key == "suggestion" and not include_suggestions:
                continue
            card = self._get_verified(peer)
            verified[peer.key] = card.trace_details()
        return verified

    def get(self, key: str) -> VerifiedAgentCard:
        if not self.settings.agent_card_discovery_enabled:
            raise AgentDiscoveryError("agent card discovery is disabled; A2A delegation requires verified cards")
        peer = next((peer for peer in self.peers if peer.key == key), None)
        if peer is None:
            raise AgentDiscoveryError(f"{key} agent card is not available")
        return self._get_verified(peer)

    def _get_verified(self, peer: PeerAgent) -> VerifiedAgentCard:
        now = time.monotonic()
        cached = self._cache.get(peer.key)
        if cached and now - cached.verified_at_monotonic < self.settings.agent_card_refresh_seconds:
            return cached
        try:
            verified = self._fetch_and_validate(peer, now)
            self._cache[peer.key] = verified
            return verified
        except (httpx.HTTPError, ValueError, AgentDiscoveryError) as error:
            if cached and now - cached.verified_at_monotonic <= self.settings.agent_card_max_stale_seconds:
                LOGGER.warning("agent_card_refresh_failed_using_cached_card agent=%s error=%s", peer.key, error)
                return cached
            LOGGER.warning("agent_card_discovery_failed agent=%s card_url=%s error=%s", peer.key, peer.card_url, error)
            raise AgentDiscoveryError(f"{peer.key} agent card is unavailable or incompatible") from error

    def _fetch_and_validate(self, peer: PeerAgent, verified_at: float) -> VerifiedAgentCard:
        if not peer.card_url:
            raise AgentDiscoveryError(f"{peer.key} agent card URL is not configured")
        response = self.client.get(peer.card_url)
        response.raise_for_status()
        card = AgentCard.model_validate(response.json())
        if not any(skill.id == peer.required_skill for skill in card.skills):
            raise AgentDiscoveryError(f"{peer.key} agent card does not advertise {peer.required_skill}")
        if not card.protocol_version or not card.protocol_version.startswith("1."):
            raise AgentDiscoveryError(f"{peer.key} agent card does not support A2A v1")
        extension = card.sllp or {}
        if extension.get("contractVersion") != "2.0":
            raise AgentDiscoveryError(f"{peer.key} agent card does not support SLLP contract v2.0")
        if self._origin(card.url) != self._origin(peer.card_url):
            raise AgentDiscoveryError(f"{peer.key} agent card endpoint differs from the allowlisted card origin")
        return VerifiedAgentCard(
            key=peer.key,
            name=card.name,
            card_url=peer.card_url,
            service_url=card.url,
            skill=peer.required_skill,
            contract_version="2.0",
            verified_at_monotonic=verified_at,
        )

    @staticmethod
    def _origin(url: str) -> tuple[str, str, int | None]:
        parsed = urlparse(url)
        if parsed.scheme not in {"http", "https"} or not parsed.hostname:
            raise AgentDiscoveryError("agent URL must be an absolute HTTP(S) URL")
        return parsed.scheme, parsed.hostname.lower(), parsed.port
