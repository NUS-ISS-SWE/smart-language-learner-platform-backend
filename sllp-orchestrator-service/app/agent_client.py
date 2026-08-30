from __future__ import annotations

import time
from collections.abc import Callable
from typing import Any

import httpx

from app.agent_providers import A2AAgentProvider, RetryingAgentProvider
from app.agent_registry import AgentRegistry
from app.agent_types import AgentAttempt, AgentCallError, AgentInvocation, T
from app.config import Settings
from app.placeholder_provider import PlaceholderAgentProvider

__all__ = ["AgentAttempt", "AgentCallError", "AgentClient", "AgentInvocation"]


class AgentClient:
    """Small facade that selects the configured assessment transport."""

    def __init__(
        self,
        settings: Settings,
        registry: AgentRegistry | None = None,
        client: httpx.Client | None = None,
        sleeper: Callable[[float], None] = time.sleep,
    ):
        self.client: httpx.Client | None = None
        if settings.agent_provider == "placeholder":
            self.provider = PlaceholderAgentProvider()
        else:
            self.client = client or httpx.Client(timeout=settings.http_timeout_seconds)
            self.provider = self._create_a2a_provider(settings, registry, self.client, sleeper)

    @staticmethod
    def _create_a2a_provider(
        settings: Settings,
        registry: AgentRegistry | None,
        client: httpx.Client,
        sleeper: Callable[[float], None],
    ) -> RetryingAgentProvider:
        if settings.agent_provider == "a2a" and registry is not None:
            return A2AAgentProvider(settings, registry, client, sleeper)
        raise ValueError("AGENT_PROVIDER must be placeholder or a2a, with the required A2A configuration")

    def close(self) -> None:
        if self.client is not None:
            self.client.close()

    def ensure_ready(self) -> object | None:
        return self.provider.ensure_ready()

    def grade(self, payload: dict[str, Any], parser: type[T]) -> AgentInvocation[T]:
        return self.provider.invoke("grading", payload, parser)

    def review(self, payload: dict[str, Any], parser: type[T]) -> AgentInvocation[T]:
        return self.provider.invoke("review", payload, parser)

    def suggest(self, payload: dict[str, Any], parser: type[T]) -> AgentInvocation[T]:
        return self.provider.invoke("suggestion", payload, parser)
