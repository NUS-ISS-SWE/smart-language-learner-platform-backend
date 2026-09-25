from __future__ import annotations

from typing import Any
from uuid import UUID

from app.models import (
    GradingCompletionEvent,
    GradingResponse,
    ReviewResponse,
    SuggestionResponse,
    SuggestionStatus,
    WorkflowSubmission,
)

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
    return report, completion_event(
        submission,
        workflow_id,
        grading.overall_score,
        render_feedback(grading, suggestions),
        True,
        suggestions,
    )


def failed_assessment(submission: WorkflowSubmission, workflow_id: UUID) -> dict[str, Any]:
    return completion_event(submission, workflow_id, None, None, False, None)


def completion_event(
    submission: WorkflowSubmission,
    workflow_id: UUID,
    score: float | None,
    feedback: str | None,
    success: bool,
    suggestions: SuggestionResponse | None,
) -> dict[str, Any]:
    return GradingCompletionEvent.from_submission(
        submission,
        workflow_id,
        score,
        feedback,
        success,
        None if success else SAFE_FAILURE_MESSAGE,
        suggestions.suggestion_status if suggestions is not None else None,
        [item.model_dump(by_alias=True, mode="json") for item in suggestions.suggestions] if suggestions is not None else [],
    ).to_kafka_payload()


def render_feedback(grading: GradingResponse, suggestions: SuggestionResponse) -> str:
    heading = "Writing assessment"
    if grading.model_version == "placeholder-v1":
        heading = "Development placeholder assessment — not an AI-generated result"
    lines = [heading, f"Overall score: {grading.overall_score:.0f}/100", "", "Rubric score breakdown:"]
    lines.extend(f"- {name}: {score:.0f}/100" for name, score in grading.rubric_scores.items())
    lines.extend(["", "Evidence-based assessment:"])
    for criterion in grading.criteria:
        excerpts = "; ".join(evidence.excerpt for evidence in criterion.evidence)
        lines.append(f"- {criterion.criterion} ({criterion.score:.0f}/100): {criterion.rationale} Evidence: {excerpts}")
    lines.extend(["", "Review outcome: Approved", "", "Improvement suggestions:"])
    if suggestions.suggestion_status is not SuggestionStatus.AVAILABLE:
        return "\n".join(lines)
    for suggestion in suggestions.suggestions:
        lines.append(f"- [{suggestion.priority}] {suggestion.criterion}: {suggestion.recommendation}")
        if suggestion.example_revision:
            lines.append(f"  Example: {suggestion.example_revision}")
        if suggestion.learning_tip:
            lines.append(f"  Learning tip: {suggestion.learning_tip}")
    return "\n".join(lines)
