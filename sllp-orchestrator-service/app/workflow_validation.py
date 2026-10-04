from __future__ import annotations

from decimal import Decimal, InvalidOperation
from typing import Any

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


def validate_grading_snapshot(grading: dict[str, Any], evidence: dict[str, Any]) -> str | None:
    """Ensure the grading artifact uses the immutable rubric captured by Evidence."""
    snapshot = evidence.get("rubricSnapshot")
    if not isinstance(snapshot, dict):
        return "missing_rubric_snapshot"
    version = snapshot.get("version")
    criteria = snapshot.get("criteria")
    if not isinstance(version, str) or not isinstance(criteria, list):
        return "invalid_rubric_snapshot"

    expected = {item.get("name") for item in criteria if isinstance(item, dict)}
    actual_criteria = {
        item.get("criterion") for item in grading.get("criteria", []) if isinstance(item, dict)
    }
    rubric_scores = grading.get("rubricScores")
    if not isinstance(rubric_scores, dict):
        return "invalid_rubric_score"
    if grading.get("rubricVersion") != version:
        return "rubric_version_mismatch"
    if not expected or actual_criteria != expected or set(rubric_scores) != expected:
        return "rubric_criteria_mismatch"

    weights = {
        item["name"]: item.get("weight")
        for item in criteria
        if isinstance(item, dict) and "name" in item
    }
    entries = grading.get("criteria")
    if not isinstance(entries, list):
        return "invalid_rubric_score"
    scores = {item["criterion"]: item for item in entries if isinstance(item, dict) and "criterion" in item}
    if len(scores) != len(entries):
        return "rubric_criteria_mismatch"

    try:
        expected_overall = Decimal("0")
        for criterion in expected:
            score = _finite_decimal(scores[criterion]["score"])
            band_score = int(scores[criterion]["bandScore"])
            weight = _finite_decimal(weights[criterion])
            reported_score = _finite_decimal(rubric_scores[criterion])
            if band_score not in range(1, 6) or score != Decimal(band_score * 20):
                return "rubric_score_scale_mismatch"
            if reported_score != score:
                return "rubric_score_mismatch"
            expected_overall += score * weight / Decimal("100")
        if _finite_decimal(grading["overallScore"]) != expected_overall:
            return "overall_score_mismatch"
    except (KeyError, TypeError, ValueError, InvalidOperation):
        return "invalid_rubric_score"
    return None


def _finite_decimal(value: Any) -> Decimal:
    decimal = Decimal(str(value))
    if not decimal.is_finite():
        raise ValueError("score must be finite")
    return decimal
