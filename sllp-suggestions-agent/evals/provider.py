"""Promptfoo provider for the production Suggestions Agent core logic.

Synthetic cases supply a fixed learner profile and approved rubric snapshot so
the run measures suggestion generation and validation without live User/Admin
services. The real OpenAI generator and moderation call are still used.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

import httpx

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.config import Settings
from app.framework import FixtureRubricFrameworkProvider
from app.generator import OpenAIChatGenerator
from app.models import LearnerProfile, SuggestionRequest
from app.service import SuggestionService, SuggestionServiceError

CASES = {
    case["id"]: case
    for case in json.loads(Path(__file__).with_name("cases.json").read_text(encoding="utf-8"))
}


class FixedProfileTool:
    def __init__(self, difficulty_level: str):
        self.difficulty_level = difficulty_level

    def get(self, user_id: int) -> LearnerProfile:
        return LearnerProfile(id=user_id, difficultyLevel=self.difficulty_level)


def call_api(prompt: str, options: dict, context: dict) -> dict:
    case_id = str(context.get("vars", {}).get("caseId") or prompt).strip()
    case = CASES.get(case_id)
    if case is None:
        return {"error": f"Unknown Suggestions evaluation case: {case_id}"}

    settings = Settings.from_environment()
    if not settings.openai_api_key:
        return {"error": "Set SUGGESTIONS_OPENAI_API_KEY or OPENAI_API_KEY before running live evaluations"}

    request = SuggestionRequest.model_validate(case["request"])
    with httpx.Client(timeout=settings.http_timeout_seconds) as client:
        generator = OpenAIChatGenerator(
            settings.openai_api_key,
            settings.openai_base_url,
            settings.openai_model,
            settings.openai_temperature,
            client,
            provider=settings.usage_provider,
            pricing_version=settings.pricing_version,
            input_price_micros_per_million=settings.input_price_micros_per_million,
            cached_input_price_micros_per_million=settings.cached_input_price_micros_per_million,
            output_price_micros_per_million=settings.output_price_micros_per_million,
            max_completion_tokens=settings.max_completion_tokens,
        )
        service = SuggestionService(
            FixedProfileTool(case["difficultyLevel"]),
            FixtureRubricFrameworkProvider(request.rubric_snapshot),
            generator,
        )
        try:
            result = service.generate(request)
        except SuggestionServiceError as error:
            return {"error": f"Suggestions Agent rejected {case_id}: {error}"}

    response: dict = {"output": result.model_dump_json(by_alias=True)}
    if result.usage:
        response["tokenUsage"] = {
            "total": result.usage.total_tokens,
            "prompt": result.usage.prompt_tokens,
            "completion": result.usage.completion_tokens,
        }
    return response
