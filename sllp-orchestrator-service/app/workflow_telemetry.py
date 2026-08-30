from __future__ import annotations

from typing import Any

from app.agent_types import AgentAttempt, AgentInvocation


def agent_attempt_entries(agent: str, attempts: list[AgentAttempt]) -> list[dict[str, Any]]:
    return [
        {
            "agent": agent,
            "agentAttempt": item.number,
            "status": item.status,
            "httpStatus": item.status_code,
            "error": item.error,
            "latencyMs": item.latency_ms,
        }
        for item in attempts
    ]


def response_metadata(payload: dict[str, Any], invocation: AgentInvocation[Any]) -> dict[str, Any]:
    metadata = {
        "modelVersion": payload.get("modelVersion"),
        "promptVersion": payload.get("promptVersion"),
        "confidence": payload.get("confidence"),
        "tokenUsage": payload.get("tokenUsage"),
    }
    metadata["provider"] = invocation.provider
    identifier_key = "a2aTaskId" if invocation.provider == "a2a" else "localInvocationId"
    context_key = "a2aContextId" if invocation.provider == "a2a" else "workflowContextId"
    metadata[identifier_key] = invocation.task_id
    metadata[context_key] = invocation.context_id
    return metadata
