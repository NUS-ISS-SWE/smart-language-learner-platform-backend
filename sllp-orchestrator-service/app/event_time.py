from __future__ import annotations

from datetime import datetime, timezone


def composition_service_timestamp(value: datetime | None = None) -> str:
    """Format a timestamp for the Java ``LocalDateTime`` Kafka contract in UTC."""
    timestamp = value or datetime.now(timezone.utc)
    if timestamp.tzinfo is not None:
        timestamp = timestamp.astimezone(timezone.utc).replace(tzinfo=None)
    return timestamp.isoformat()
