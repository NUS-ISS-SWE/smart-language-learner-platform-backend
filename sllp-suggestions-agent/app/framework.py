from __future__ import annotations

from typing import Protocol

from app.models import DeepFramework


class FrameworkUnavailable(RuntimeError):
    pass


class DeepFrameworkProvider(Protocol):
    """Port for the team-owned canonical DEEP/MOE framework source."""

    def get(self, difficulty_level: str) -> DeepFramework:
        """Return the active DEEP profile for one learner level."""


class UnavailableDeepFrameworkProvider:
    """Safe default until the DEEP owner exposes the canonical source."""

    def get(self, difficulty_level: str) -> DeepFramework:
        raise FrameworkUnavailable(
            "the canonical DEEP framework is not configured; suggestions are not ready for production"
        )


class FixtureDeepFrameworkProvider:
    """Test-only provider. Do not select it from production environment settings."""

    def __init__(self, framework: DeepFramework):
        self.framework = framework

    def get(self, difficulty_level: str) -> DeepFramework:
        return self.framework
