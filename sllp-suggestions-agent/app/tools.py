from __future__ import annotations

import httpx

from app.models import LearnerProfile


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
            return LearnerProfile.model_validate(response.json())
        except (httpx.HTTPError, ValueError) as error:
            raise ToolUnavailable("learner profile is unavailable") from error
