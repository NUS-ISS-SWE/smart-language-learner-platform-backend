from __future__ import annotations

from dataclasses import dataclass
from typing import Generic, TypeVar

from pydantic import BaseModel


T = TypeVar("T", bound=BaseModel)


@dataclass(frozen=True)
class AgentAttempt:
    number: int
    status: str
    latency_ms: int
    status_code: int | None = None
    error: str | None = None


@dataclass(frozen=True)
class AgentInvocation(Generic[T]):
    response: T
    attempts: tuple[AgentAttempt, ...]
    task_id: str
    context_id: str
    provider: str


class AgentCallError(RuntimeError):
    """A safe, workflow-facing failure from an assessment provider."""

    def __init__(self, agent: str, message: str, attempts: tuple[AgentAttempt, ...] = ()):
        super().__init__(message)
        self.agent = agent
        self.attempts = attempts
