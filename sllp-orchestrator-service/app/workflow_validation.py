from __future__ import annotations

from app.config import Settings
from app.models import WorkflowSubmission


def validate_submission(submission: WorkflowSubmission, settings: Settings) -> str | None:
    composition = submission.composition
    if composition.composition_id < 1 or composition.user_id < 1:
        return "composition and user identifiers must be positive"
    if not composition.title.strip() or not composition.content.strip():
        return "title and composition content are required"
    if composition.language not in {"ENGLISH", "CHINESE"}:
        return "only ENGLISH and CHINESE compositions are supported"
    if len(composition.content) > settings.max_composition_characters:
        return "composition exceeds the permitted size"
    return None
