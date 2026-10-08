from __future__ import annotations

import httpx

from app.models import LearnerProfile
from app.metrics import record_upstream


class ToolUnavailable(RuntimeError):
    pass


class LearnerProfileTool:
    """Read-only, allowlisted lookup of the learner level used by the rubric."""

    def __init__(self, base_url: str, client: httpx.Client):
        self.base_url = base_url.rstrip("/")
        self.client = client

    def get(self, user_id: int) -> LearnerProfile:
        try:
            response = self.client.get(f"{self.base_url}/api/v1/auth/profile/{user_id}")
            response.raise_for_status()
            profile = LearnerProfile.model_validate(response.json())
            record_upstream("learner_profile", True)
            return profile
        except (httpx.HTTPError, ValueError) as error:
            record_upstream("learner_profile", False)
            raise ToolUnavailable("learner profile is unavailable") from error
