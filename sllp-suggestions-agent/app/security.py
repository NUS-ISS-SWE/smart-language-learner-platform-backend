from __future__ import annotations

import hmac


def bearer_token_matches(authorization: str, expected_token: str) -> bool:
    """Compare credentials without prefix-match timing differences."""
    return bool(expected_token) and hmac.compare_digest(authorization, f"Bearer {expected_token}")
