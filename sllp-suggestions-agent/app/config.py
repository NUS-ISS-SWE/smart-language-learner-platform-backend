from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    public_base_url: str
    user_service_url: str
    admin_service_url: str
    a2a_token: str
    openai_api_key: str | None
    openai_base_url: str
    openai_model: str
    openai_temperature: float
    http_timeout_seconds: float
    usage_provider: str
    pricing_version: str
    input_price_micros_per_million: int
    cached_input_price_micros_per_million: int
    output_price_micros_per_million: int
    max_a2a_request_bytes: int = 262_144
    max_completion_tokens: int = 500

    def __post_init__(self) -> None:
        if self.max_a2a_request_bytes <= 0:
            raise ValueError("SUGGESTIONS_MAX_A2A_REQUEST_BYTES must be positive")
        if self.max_completion_tokens <= 0:
            raise ValueError("SUGGESTIONS_MAX_COMPLETION_TOKENS must be positive")

    @classmethod
    def from_environment(cls) -> "Settings":
        return cls(
            public_base_url=os.getenv("SUGGESTIONS_PUBLIC_BASE_URL", "http://localhost:8091").rstrip("/"),
            user_service_url=os.getenv("SUGGESTIONS_USER_SERVICE_URL", "http://localhost:8082").rstrip("/"),
            admin_service_url=os.getenv("SUGGESTIONS_ADMIN_SERVICE_URL", "http://localhost:8086").rstrip("/"),
            a2a_token=os.getenv("SUGGESTION_AGENT_TOKEN", ""),
            openai_api_key=os.getenv("SUGGESTIONS_OPENAI_API_KEY") or os.getenv("OPENAI_API_KEY"),
            openai_base_url=os.getenv("SUGGESTIONS_OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/"),
            openai_model=os.getenv("SUGGESTIONS_OPENAI_MODEL", "gpt-4o-mini"),
            openai_temperature=float(os.getenv("SUGGESTIONS_OPENAI_TEMPERATURE", "0.2")),
            http_timeout_seconds=float(os.getenv("SUGGESTIONS_HTTP_TIMEOUT_SECONDS", "20")),
            usage_provider=os.getenv("SUGGESTIONS_USAGE_PROVIDER", "OPENAI"),
            pricing_version=os.getenv("SUGGESTIONS_PRICING_VERSION", "openai-gpt-4o-mini-2026-08"),
            input_price_micros_per_million=int(os.getenv("SUGGESTIONS_INPUT_PRICE_MICROS_PER_MILLION", "150000")),
            cached_input_price_micros_per_million=int(os.getenv("SUGGESTIONS_CACHED_INPUT_PRICE_MICROS_PER_MILLION", "75000")),
            output_price_micros_per_million=int(os.getenv("SUGGESTIONS_OUTPUT_PRICE_MICROS_PER_MILLION", "600000")),
            max_a2a_request_bytes=int(os.getenv("SUGGESTIONS_MAX_A2A_REQUEST_BYTES", "262144")),
            max_completion_tokens=int(os.getenv("SUGGESTIONS_MAX_COMPLETION_TOKENS", "500")),
        )
