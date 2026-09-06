from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    public_base_url: str
    user_service_url: str
    openai_api_key: str | None
    openai_base_url: str
    openai_model: str
    openai_temperature: float
    http_timeout_seconds: float
    deep_framework_provider: str

    @classmethod
    def from_environment(cls) -> "Settings":
        return cls(
            public_base_url=os.getenv("SUGGESTIONS_PUBLIC_BASE_URL", "http://localhost:8091").rstrip("/"),
            user_service_url=os.getenv("SUGGESTIONS_USER_SERVICE_URL", "http://localhost:8082").rstrip("/"),
            openai_api_key=os.getenv("SUGGESTIONS_OPENAI_API_KEY") or os.getenv("OPENAI_API_KEY"),
            openai_base_url=os.getenv("SUGGESTIONS_OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/"),
            openai_model=os.getenv("SUGGESTIONS_OPENAI_MODEL", "gpt-4o-mini"),
            openai_temperature=float(os.getenv("SUGGESTIONS_OPENAI_TEMPERATURE", "0.2")),
            http_timeout_seconds=float(os.getenv("SUGGESTIONS_HTTP_TIMEOUT_SECONDS", "20")),
            # No production DEEP source exists yet. This default deliberately prevents
            # a deployment from serving invented framework guidance.
            deep_framework_provider=os.getenv("DEEP_FRAMEWORK_PROVIDER", "unavailable").strip().lower(),
        )
