from __future__ import annotations

import re


_PII_PATTERNS = (
    re.compile(r"\b[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}\b"),
    re.compile(r"\b(?:\+?\d[\d -]{7,}\d)\b"),
)


def redact_for_trace(value: object) -> str:
    """Keep operational logs useful without persisting contact details or source text."""
    redacted = str(value)
    for pattern in _PII_PATTERNS:
        redacted = pattern.sub("[REDACTED]", redacted)
    return redacted[:2_000]

