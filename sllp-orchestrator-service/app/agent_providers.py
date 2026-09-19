from __future__ import annotations

import logging
import time
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any
from uuid import uuid4

import httpx
from pydantic import BaseModel, ValidationError

from app.agent_registry import AgentRegistry
from app.agent_types import AgentAttempt, AgentCallError, AgentInvocation, T
from app.config import Settings
from app.provider_models import A2ASendMessageRequest, AgentName
from app.security import redact_for_trace

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class ProviderResponse:
    payload: dict[str, Any]
    status_code: int
    task_id: str
    context_id: str


class RetryingAgentProvider:
    """Provider-neutral retry and output-validation policy."""

    name: str

    def __init__(self, settings: Settings, client: httpx.Client, sleeper: Callable[[float], None]):
        self.settings = settings
        self.client = client
        self.sleeper = sleeper

    def invoke(self, agent: AgentName, payload: dict[str, Any], parser: type[T]) -> AgentInvocation[T]:
        attempts: list[AgentAttempt] = []
        last_error: Exception | None = None
        for retry in range(self.settings.max_agent_retries + 1):
            started = time.monotonic()
            try:
                return self._success(agent, parser, self._request(agent, payload, parser, retry), attempts, retry, started)
            except AgentCallError as error:
                raise self._terminal_error(agent, error, attempts, retry, started, "PROTOCOL_ERROR") from error
            except (ValidationError, ValueError) as error:
                message = f"{agent} returned an invalid response"
                raise self._terminal_error(agent, error, attempts, retry, started, "INVALID_RESPONSE", message) from error
            except (httpx.TimeoutException, httpx.NetworkError, httpx.HTTPStatusError) as error:
                last_error = error
                if self._should_stop(retry, error):
                    attempts.append(self._attempt(retry, "FAILED", started, error))
                    break
                attempts.append(self._attempt(retry, "RETRYING", started, error))
                self.sleeper(2**retry)
        raise AgentCallError(agent, f"{agent} is temporarily unavailable", tuple(attempts)) from last_error

    def _success(
        self,
        agent: AgentName,
        parser: type[T],
        response: ProviderResponse,
        attempts: list[AgentAttempt],
        retry: int,
        started: float,
    ) -> AgentInvocation[T]:
        parsed = parser.model_validate(response.payload)
        latency_ms = int((time.monotonic() - started) * 1000)
        attempts.append(AgentAttempt(retry + 1, "SUCCEEDED", latency_ms, response.status_code))
        LOGGER.info("agent_completed provider=%s agent=%s latency_ms=%d retry=%d", self.name, agent, latency_ms, retry)
        return AgentInvocation(parsed, tuple(attempts), response.task_id, response.context_id, self.name)

    def _terminal_error(
        self,
        agent: AgentName,
        error: Exception,
        attempts: list[AgentAttempt],
        retry: int,
        started: float,
        status: str,
        message: str | None = None,
    ) -> AgentCallError:
        attempts.append(self._attempt(retry, status, started, error))
        return AgentCallError(agent, message or str(error), tuple(attempts))

    def _request(self, agent: AgentName, payload: dict[str, Any], parser: type[BaseModel], retry: int) -> ProviderResponse:
        raise NotImplementedError

    def _attempt(self, retry: int, status: str, started: float, error: Exception) -> AgentAttempt:
        response = error.response if isinstance(error, httpx.HTTPStatusError) else None
        return AgentAttempt(
            retry + 1,
            status,
            int((time.monotonic() - started) * 1000),
            response.status_code if response is not None else None,
            redact_for_trace(error),
        )

    def _should_stop(self, retry: int, error: Exception) -> bool:
        return retry == self.settings.max_agent_retries or (
            isinstance(error, httpx.HTTPStatusError) and error.response is not None and error.response.status_code < 500
        )


class A2AAgentProvider(RetryingAgentProvider):
    name = "a2a"

    def __init__(self, settings: Settings, registry: AgentRegistry, client: httpx.Client, sleeper: Callable[[float], None]):
        super().__init__(settings, client, sleeper)
        self.registry = registry

    def ensure_ready(self, include_suggestions: bool = True) -> dict[str, dict[str, str]]:
        return self.registry.ensure_ready(include_suggestions=include_suggestions)

    def _request(self, agent: AgentName, payload: dict[str, Any], parser: type[BaseModel], retry: int) -> ProviderResponse:
        request_id = str(uuid4())
        workflow_id = payload["workflowId"]
        request = A2ASendMessageRequest.for_assessment(request_id, workflow_id, payload)
        response = self.client.post(
            self.registry.get(agent).service_url,
            json=request.model_dump(by_alias=True, mode="json"),
            headers={"A2A-Version": "1.0"},
        )
        _raise_on_server_error(response, "agent returned a server error")
        task, artifact = extract_completed_artifact(response.json(), agent)
        return ProviderResponse(artifact, response.status_code, task["id"], task.get("contextId", workflow_id))


def _raise_on_server_error(response: httpx.Response, message: str) -> None:
    if response.status_code >= 500:
        raise httpx.HTTPStatusError(message, request=response.request, response=response)
    response.raise_for_status()


def extract_completed_artifact(response: object, agent: AgentName) -> tuple[dict[str, Any], dict[str, Any]]:
    task = _completed_a2a_task(response, agent)
    return task, _json_artifact(task, agent)


def _completed_a2a_task(response: object, agent: AgentName) -> dict[str, Any]:
    if not isinstance(response, dict):
        raise AgentCallError(agent, f"{agent} returned a non-object A2A response")
    _raise_a2a_error(response, agent)
    result = response.get("result")
    if not isinstance(result, dict):
        raise AgentCallError(agent, f"{agent} returned a malformed A2A result")
    task = result.get("task")
    if not isinstance(task, dict):
        raise AgentCallError(agent, f"{agent} did not return an A2A task")
    if not isinstance(task.get("id"), str) or not task["id"].strip():
        raise AgentCallError(agent, f"{agent} returned an A2A task without an id")
    status = task.get("status")
    if not isinstance(status, dict) or status.get("state") != "TASK_STATE_COMPLETED":
        raise AgentCallError(agent, f"{agent} returned non-terminal A2A task state")
    return task


def _raise_a2a_error(response: dict[str, Any], agent: AgentName) -> None:
    error = response.get("error")
    if error is None:
        return
    if not isinstance(error, dict):
        raise AgentCallError(agent, f"{agent} returned a malformed A2A error")
    message = redact_for_trace(error.get("message", ""))
    raise AgentCallError(agent, f"{agent} returned A2A error {error.get('code', 'unknown')}: {message}")


def _json_artifact(task: dict[str, Any], agent: AgentName) -> dict[str, Any]:
    artifacts = task.get("artifacts")
    if not isinstance(artifacts, list):
        raise AgentCallError(agent, f"{agent} completed without A2A artifacts")
    for artifact in artifacts:
        if not isinstance(artifact, dict):
            continue
        parts = artifact.get("parts")
        if not isinstance(parts, list):
            continue
        for part in parts:
            if isinstance(part, dict) and part.get("mediaType") == "application/json" and isinstance(part.get("data"), dict):
                return part["data"]
    raise AgentCallError(agent, f"{agent} completed without a JSON result artifact")
