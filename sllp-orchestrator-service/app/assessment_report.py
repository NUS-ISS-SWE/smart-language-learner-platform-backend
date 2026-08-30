from __future__ import annotations

from typing import Any
from uuid import UUID

from app.event_time import composition_service_timestamp
from app.models import GradingResponse, ReviewResponse, SuggestionResponse, WorkflowSubmission

SAFE_FAILURE_MESSAGE = "Your assessment is temporarily unavailable. Please try again shortly."


def completed_assessment(
    submission: WorkflowSubmission,
    workflow_id: UUID,
    grading: GradingResponse,
    review: ReviewResponse,
    suggestions: SuggestionResponse,
) -> tuple[dict[str, Any], dict[str, Any]]:
    report = {
        "schemaVersion": submission.schema_version,
        "workflowId": str(workflow_id),
        "score": grading.overall_score,
        "grading": grading.model_dump(by_alias=True, mode="json"),
        "review": review.model_dump(by_alias=True, mode="json"),
        "suggestions": suggestions.model_dump(by_alias=True, mode="json"),
    }
    return report, completion_event(submission, grading.overall_score, render_feedback(grading, suggestions), True)


def failed_assessment(submission: WorkflowSubmission) -> dict[str, Any]:
    return completion_event(submission, None, None, False)


def completion_event(
    submission: WorkflowSubmission,
    score: float | None,
    feedback: str | None,
    success: bool,
) -> dict[str, Any]:
    return {
        "compositionId": submission.composition.composition_id,
        "userId": submission.composition.user_id,
        "score": score,
        "feedback": feedback,
        "success": success,
        "failureReason": None if success else SAFE_FAILURE_MESSAGE,
        "completedAt": composition_service_timestamp(),
    }


def render_feedback(grading: GradingResponse, suggestions: SuggestionResponse) -> str:
    heading = "Writing assessment"
    if grading.model_version == "placeholder-v1":
        heading = "Development placeholder assessment — not an AI-generated result"
    lines = [heading, f"Overall score: {grading.overall_score:.0f}/100", "", "DEEP score breakdown:"]
    lines.extend(f"- {name}: {score:.0f}/100" for name, score in grading.deep_scores.items())
    lines.extend(["", "Evidence-based assessment:"])
    for criterion in grading.criteria:
        excerpts = "; ".join(evidence.excerpt for evidence in criterion.evidence)
        lines.append(f"- {criterion.criterion} ({criterion.score:.0f}/100): {criterion.rationale} Evidence: {excerpts}")
    lines.extend(["", "Review outcome: Approved", "", "Improvement suggestions:"])
    for suggestion in suggestions.suggestions:
        lines.append(f"- [{suggestion.priority}] {suggestion.criterion}: {suggestion.recommendation}")
        if suggestion.example_revision:
            lines.append(f"  Example: {suggestion.example_revision}")
        if suggestion.learning_tip:
            lines.append(f"  Learning tip: {suggestion.learning_tip}")
    return "\n".join(lines)
