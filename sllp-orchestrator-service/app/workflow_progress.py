from __future__ import annotations

from typing import NotRequired, TypedDict
from uuid import UUID

from app.event_time import composition_service_timestamp
from app.models import WorkflowStatus, WorkflowSubmission


class ProgressEvent(TypedDict):
    schemaVersion: str
    workflowId: str
    compositionId: int
    userId: int
    stage: str
    message: str
    updatedAt: str
    traceId: NotRequired[str]


PROGRESS_MESSAGES = {
    WorkflowStatus.EVIDENCING: "Gathering supporting passages from your composition…",
    WorkflowStatus.GRADING: "Assessing your composition…",
    WorkflowStatus.REGRADING: "Double-checking your assessment for accuracy…",
    WorkflowStatus.REVIEWING: "Checking the assessment for accuracy…",
    WorkflowStatus.SUGGESTING: "Creating personalised improvement suggestions…",
}


def progress_event(workflow_id: UUID, submission: WorkflowSubmission, status: WorkflowStatus) -> ProgressEvent | None:
    message = PROGRESS_MESSAGES.get(status)
    if message is None:
        return None
    composition = submission.composition
    return {
        "schemaVersion": "1.0",
        "workflowId": str(workflow_id),
        "compositionId": composition.composition_id,
        "userId": composition.user_id,
        "stage": status.value,
        "message": message,
        "updatedAt": composition_service_timestamp(),
    }
