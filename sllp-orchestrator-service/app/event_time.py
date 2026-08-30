from __future__ import annotations

from datetime import datetime, timezone


def composition_service_timestamp() -> str:
    """Return a UTC wall-clock timestamp for the Java LocalDateTime event contract."""
    return datetime.now(timezone.utc).replace(tzinfo=None).isoformat()
