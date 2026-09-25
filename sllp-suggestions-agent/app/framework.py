from __future__ import annotations

import hashlib
import json
from typing import Protocol

import httpx

from app.models import RubricCriterion, RubricFramework


class FrameworkUnavailable(RuntimeError):
    pass


class RubricFrameworkProvider(Protocol):
    """Read-only source for the canonical Admin assessment rubric."""

    def get(self, language: str, difficulty_level: str) -> RubricFramework:
        """Return the active rubric profile for one language and learner level."""


class AdminRubricFrameworkProvider:
    def __init__(self, base_url: str, client: httpx.Client):
        self.base_url = base_url.rstrip("/")
        self.client = client

    def get(self, language: str, difficulty_level: str) -> RubricFramework:
        try:
            response = self.client.get(f"{self.base_url}/api/v1/admin/rubrics", params={"language": language, "difficultyLevel": difficulty_level})
            response.raise_for_status()
            rows = response.json()
            criteria = [RubricCriterion(name=row["criterionName"], weight=float(row["weightPercentage"]), guidance=row["criterionDescription"]) for row in rows]
            raw = json.dumps([item.model_dump(by_alias=True) for item in criteria], sort_keys=True, ensure_ascii=False)
            return RubricFramework(version=f"admin-rubric-{hashlib.sha256(raw.encode()).hexdigest()[:12]}", criteria=criteria)
        except (httpx.HTTPError, KeyError, TypeError, ValueError) as error:
            raise FrameworkUnavailable("the canonical Admin rubric is unavailable") from error


class FixtureRubricFrameworkProvider:
    """Test-only provider. Do not select it from production environment settings."""

    def __init__(self, framework: RubricFramework):
        self.framework = framework

    def get(self, language: str, difficulty_level: str) -> RubricFramework:
        return self.framework


FixtureDeepFrameworkProvider = FixtureRubricFrameworkProvider
