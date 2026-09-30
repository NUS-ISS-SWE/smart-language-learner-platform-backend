from __future__ import annotations

from prometheus_client import Counter, Histogram


A2A_REQUESTS = Counter("sllp_suggestions_a2a_requests_total", "A2A suggestion requests by safe outcome.", ("outcome",))
A2A_DURATION = Histogram("sllp_suggestions_a2a_request_duration_seconds", "End-to-end A2A suggestion request duration.")
UPSTREAM_CALLS = Counter("sllp_suggestions_upstream_calls_total", "Upstream calls by dependency and outcome.", ("dependency", "outcome"))
TOKENS = Counter("sllp_suggestions_tokens_total", "Provider-reported tokens used by suggestions.", ("kind",))
ESTIMATED_COST_MICROS = Counter("sllp_suggestions_estimated_cost_micros_total", "Estimated USD micros consumed by suggestions.")
SUGGESTIONS = Counter("sllp_suggestions_generated_total", "Learner suggestions emitted by priority.", ("priority",))


def record_upstream(dependency: str, succeeded: bool) -> None:
    UPSTREAM_CALLS.labels(dependency=dependency, outcome="success" if succeeded else "failure").inc()


def record_result(result: object) -> None:
    """Emit only bounded, non-identifying labels from a completed artifact."""
    usage = getattr(result, "usage", None)
    if usage is not None:
        TOKENS.labels(kind="prompt").inc(usage.prompt_tokens)
        TOKENS.labels(kind="cached_input").inc(usage.cached_input_tokens)
        TOKENS.labels(kind="completion").inc(usage.completion_tokens)
        non_cached = usage.prompt_tokens - usage.cached_input_tokens
        micros = (non_cached * usage.input_price_micros_per_million + usage.cached_input_tokens * usage.cached_input_price_micros_per_million + usage.completion_tokens * usage.output_price_micros_per_million) // 1_000_000
        ESTIMATED_COST_MICROS.inc(micros)
    for suggestion in getattr(result, "suggestions", []):
        SUGGESTIONS.labels(priority=suggestion.priority).inc()
