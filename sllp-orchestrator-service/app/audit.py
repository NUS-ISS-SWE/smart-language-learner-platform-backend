"""Best-effort, redacted audit ingestion for background workflow activity."""
from __future__ import annotations

import hashlib
import hmac
import json
import logging
import time
import urllib.request
from datetime import datetime, timezone
from uuid import uuid4

from app.config import Settings

LOGGER = logging.getLogger(__name__)


def publish(settings: Settings, action: str, outcome: str, target_type: str, target_id: str, correlation_id: str | None = None) -> None:
    if not settings.audit_ingest_secret:
        return
    event = {"id": str(uuid4()), "occurredAt": datetime.now(timezone.utc).isoformat(), "service": "orchestrator-service", "category": "SYSTEM_WORKFLOW", "action": action, "outcome": outcome, "actorType": "SYSTEM", "targetType": target_type, "targetId": target_id, "correlationId": correlation_id, "metadata": {}}
    body = json.dumps(event, separators=(",", ":")).encode()
    timestamp = str(int(time.time()))
    signature = hmac.new(settings.audit_ingest_secret.encode(), timestamp.encode() + b"." + body, hashlib.sha256).hexdigest()
    request = urllib.request.Request(settings.audit_ingest_url, data=body, method="POST", headers={"Content-Type": "application/json", "X-Sllp-Audit-Timestamp": timestamp, "X-Sllp-Audit-Signature": signature})
    try:
        with urllib.request.urlopen(request, timeout=2):
            pass
    except Exception:
        LOGGER.exception("audit_write_failed action=%s target_id=%s", action, target_id)
