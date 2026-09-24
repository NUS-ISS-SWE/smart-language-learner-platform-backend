from __future__ import annotations

import os
from dataclasses import dataclass


def _bool(name: str, default: bool) -> bool:
    return os.getenv(name, str(default)).strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    database_url: str
    kafka_bootstrap_servers: str
    kafka_group_id: str
    grading_requested_topic: str
    grading_completed_topic: str
    grading_progress_topic: str
    http_timeout_seconds: float
    max_agent_retries: int
    max_regrades: int
    minimum_confidence: float
    max_composition_characters: int
    kafka_enabled: bool
    agent_card_discovery_enabled: bool
    agent_card_refresh_seconds: int
    agent_card_max_stale_seconds: int
    grading_agent_card_url: str | None
    review_agent_card_url: str | None
    suggestion_agent_card_url: str | None
    evidence_agent_card_url: str | None = None
    agent_provider: str = "placeholder"
    evidence_agent_token: str = ""
    grading_agent_token: str = ""
    review_agent_token: str = ""
    suggestion_agent_token: str = ""
    workflow_poll_interval_seconds: float = 0.5
    outbox_poll_interval_seconds: float = 0.25
    worker_lease_seconds: int = 60
    outbox_lease_seconds: int = 30
    worker_heartbeat_seconds: int = 15
    otel_exporter_otlp_endpoint: str | None = None
    audit_ingest_url: str = "http://localhost:8086/internal/audit-events"
    audit_ingest_secret: str = ""

    @classmethod
    def from_environment(cls) -> "Settings":
        return cls(
            database_url=os.getenv(
                "ORCHESTRATOR_DATABASE_URL",
                "postgresql+psycopg://postgres:password@localhost:5432/sllp_main_db",
            ),
            kafka_bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092"),
            kafka_group_id=os.getenv("KAFKA_GROUP_ID", "sllp-orchestrator-group"),
            grading_requested_topic=os.getenv(
                "KAFKA_GRADING_REQUESTED_TOPIC", "SLLP_COMPOSITION_GRADING_REQUESTED_TOPIC"
            ),
            grading_completed_topic=os.getenv(
                "KAFKA_GRADING_COMPLETED_TOPIC", "SLLP_COMPOSITION_GRADING_COMPLETED_TOPIC"
            ),
            grading_progress_topic=os.getenv(
                "KAFKA_GRADING_PROGRESS_TOPIC", "SLLP_COMPOSITION_GRADING_PROGRESS_TOPIC"
            ),
            http_timeout_seconds=float(os.getenv("AGENT_HTTP_TIMEOUT_SECONDS", "60")),
            max_agent_retries=int(os.getenv("MAX_AGENT_RETRIES", "2")),
            max_regrades=int(os.getenv("MAX_REGRADES", "1")),
            minimum_confidence=float(os.getenv("MINIMUM_CONFIDENCE", "0.60")),
            max_composition_characters=int(os.getenv("MAX_COMPOSITION_CHARACTERS", "20000")),
            kafka_enabled=_bool("KAFKA_ENABLED", True),
            agent_card_discovery_enabled=_bool("AGENT_CARD_DISCOVERY_ENABLED", True),
            agent_card_refresh_seconds=int(os.getenv("AGENT_CARD_REFRESH_SECONDS", "300")),
            agent_card_max_stale_seconds=int(os.getenv("AGENT_CARD_MAX_STALE_SECONDS", "3600")),
            grading_agent_card_url=os.getenv("GRADING_AGENT_CARD_URL") or None,
            review_agent_card_url=os.getenv("REVIEW_AGENT_CARD_URL") or None,
            suggestion_agent_card_url=os.getenv("SUGGESTION_AGENT_CARD_URL") or None,
            evidence_agent_card_url=os.getenv("EVIDENCE_AGENT_CARD_URL") or None,
            agent_provider=os.getenv("AGENT_PROVIDER", "placeholder").strip().lower(),
            evidence_agent_token=os.getenv("EVIDENCE_AGENT_TOKEN", ""),
            grading_agent_token=os.getenv("GRADING_AGENT_TOKEN", ""),
            review_agent_token=os.getenv("REVIEW_AGENT_TOKEN", ""),
            suggestion_agent_token=os.getenv("SUGGESTION_AGENT_TOKEN", ""),
            workflow_poll_interval_seconds=float(os.getenv("WORKFLOW_POLL_INTERVAL_SECONDS", "0.5")),
            outbox_poll_interval_seconds=float(os.getenv("OUTBOX_POLL_INTERVAL_SECONDS", "0.25")),
            worker_lease_seconds=int(os.getenv("WORKER_LEASE_SECONDS", "60")),
            outbox_lease_seconds=int(os.getenv("OUTBOX_LEASE_SECONDS", "30")),
            worker_heartbeat_seconds=int(os.getenv("WORKER_HEARTBEAT_SECONDS", "15")),
            otel_exporter_otlp_endpoint=os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") or None,
            audit_ingest_url=os.getenv("AUDIT_INGEST_URL", "http://localhost:8086/internal/audit-events"),
            audit_ingest_secret=os.getenv("AUDIT_INGEST_SECRET", ""),
        )
