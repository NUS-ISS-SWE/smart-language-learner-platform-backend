from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Literal, TypedDict

from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator


def _camel_case(value: str) -> str:
    head, *tail = value.split("_")
    return head + "".join(word.capitalize() for word in tail)


class ReviewScoringEvidence(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    excerpt: str = Field(min_length=1, max_length=2_000)
    location: str | None = None


class ReviewScoringCriterion(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    criterion: str = Field(min_length=1)
    score: float = Field(ge=0, le=100)
    rationale: str = Field(min_length=1)
    evidence: list[ReviewScoringEvidence] = Field(min_length=1)


class ReviewScoringRequest(BaseModel):
    """Untrusted composition plus upstream grading and optional rubric evidence."""

    model_config = ConfigDict(extra="allow", populate_by_name=True)

    composition: dict[str, Any]
    grading: dict[str, Any]
    rubric: list[dict[str, Any]] = Field(default_factory=list)
    evidence: list[ReviewScoringEvidence] = Field(default_factory=list)

    @field_validator("composition")
    @classmethod
    def composition_has_content(cls, value: dict[str, Any]) -> dict[str, Any]:
        content = value.get("content")
        if not isinstance(content, str) or not content.strip():
            raise ValueError("composition.content must be a non-empty string")
        return value


class ReviewScoringFinding(BaseModel):
    criterion: str = Field(min_length=1)
    message: str = Field(min_length=1)
    severity: Literal["HIGH", "MEDIUM", "LOW"]


class ReviewScoringResult(BaseModel):
    """Auditable quality score for a grading result, not a replacement student score."""

    model_config = ConfigDict(populate_by_name=True, alias_generator=_camel_case)

    overall_score: float = Field(ge=0, le=100)
    score_alignment: float = Field(ge=0, le=100)
    evidence_grounding: float = Field(ge=0, le=100)
    rubric_compliance: float = Field(ge=0, le=100)
    findings: list[ReviewScoringFinding] = Field(default_factory=list)
    confidence: float = Field(ge=0, le=1)
    model_version: str = "review-scoring-deterministic-v1"
    prompt_version: str = "review-scoring-v1"
    token_usage: int = 0

    def as_artifact(self) -> dict[str, Any]:
        return self.model_dump(by_alias=True, mode="json")


class _ReviewScoringState(TypedDict, total=False):
    request: ReviewScoringRequest
    evidence_grounding: float
    rubric_compliance: float
    score_alignment: float
    findings: list[ReviewScoringFinding]
    result: ReviewScoringResult
    error: str


class ReviewScoringAgent:
    """LangGraph agent that audits grading quality in small, testable stages."""

    def __init__(self) -> None:
        self.graph = self._build_graph()

    @staticmethod
    def definition() -> dict[str, Any]:
        return {
            "name": "SLLP Review Scoring Agent",
            "protocolVersion": "1.0",
            "skills": [
                {
                    "id": "review-scoring",
                    "name": "Review scoring",
                    "description": "Audits score alignment, evidence grounding, and rubric coverage.",
                }
            ],
            "sllp": {"contractVersion": "1.0"},
        }

    def run(self, payload: Mapping[str, Any]) -> ReviewScoringResult:
        try:
            request = ReviewScoringRequest.model_validate(dict(payload))
        except ValidationError as error:
            raise ValueError("invalid review scoring request") from error

        state = self.graph.invoke({"request": request})
        if state.get("error"):
            raise ValueError(state["error"])
        return state["result"]

    def _build_graph(self):
        graph = StateGraph(_ReviewScoringState)
        graph.add_node("validate", self._validate)
        graph.add_node("analyze_evidence", self._analyze_evidence)
        graph.add_node("check_rubric", self._check_rubric)
        graph.add_node("check_consistency", self._check_consistency)
        graph.add_node("assemble", self._assemble)
        graph.add_edge(START, "validate")
        graph.add_conditional_edges("validate", self._after_validate, {"analyze_evidence": "analyze_evidence", "fail": END})
        graph.add_edge("analyze_evidence", "check_rubric")
        graph.add_edge("check_rubric", "check_consistency")
        graph.add_edge("check_consistency", "assemble")
        graph.add_edge("assemble", END)
        return graph.compile()

    @staticmethod
    def _validate(state: _ReviewScoringState) -> _ReviewScoringState:
        if state.get("request") is None:
            return {"error": "review scoring request is missing"}
        return {}

    @staticmethod
    def _after_validate(state: _ReviewScoringState) -> str:
        return "fail" if state.get("error") else "analyze_evidence"

    @staticmethod
    def _analyze_evidence(state: _ReviewScoringState) -> _ReviewScoringState:
        request = state["request"]
        content = str(request.composition["content"]).casefold()
        criteria = _grading_criteria(request.grading)
        evidence = request.evidence or [item for criterion in criteria for item in criterion.evidence]
        if not evidence:
            return {
                "evidence_grounding": 0.0,
                "findings": [ReviewScoringFinding(criterion="evidence", message="No evidence was supplied for the grading result.", severity="HIGH")],
            }

        grounded = sum(_excerpt_is_grounded(item.excerpt, content) for item in evidence)
        score = round(100 * grounded / len(evidence), 2)
        findings = []
        if score < 70:
            findings.append(ReviewScoringFinding(criterion="evidence", message="Several grading excerpts cannot be located in the submitted composition.", severity="HIGH"))
        elif score < 90:
            findings.append(ReviewScoringFinding(criterion="evidence", message="Some grading excerpts are weakly grounded in the submitted composition.", severity="MEDIUM"))
        return {"evidence_grounding": score, "findings": findings}

    @staticmethod
    def _check_rubric(state: _ReviewScoringState) -> _ReviewScoringState:
        request = state["request"]
        criteria = _grading_criteria(request.grading)
        if not request.rubric:
            return {"rubric_compliance": 100.0}

        rubric_names = {_name(item.get("criterion") or item.get("name")) for item in request.rubric}
        graded_names = {_name(item.criterion) for item in criteria}
        matched = len(rubric_names & graded_names)
        compliance = round(100 * matched / len(rubric_names), 2)
        findings = list(state.get("findings", []))
        if compliance < 100:
            findings.append(ReviewScoringFinding(criterion="rubric", message="The grading result does not cover every rubric criterion.", severity="HIGH" if compliance < 70 else "MEDIUM"))
        return {"rubric_compliance": compliance, "findings": findings}

    @staticmethod
    def _check_consistency(state: _ReviewScoringState) -> _ReviewScoringState:
        request = state["request"]
        criteria = _grading_criteria(request.grading)
        average = sum(item.score for item in criteria) / len(criteria)
        overall = _number(request.grading.get("overallScore", request.grading.get("score", 0)))
        difference = abs(average - overall)
        alignment = round(max(0.0, 100.0 - difference * 2.0), 2)
        findings = list(state.get("findings", []))
        if alignment < 80:
            findings.append(ReviewScoringFinding(criterion="score", message="The overall score is materially inconsistent with the criterion scores.", severity="HIGH"))
        return {"score_alignment": alignment, "findings": findings}

    @staticmethod
    def _assemble(state: _ReviewScoringState) -> _ReviewScoringState:
        evidence = state["evidence_grounding"]
        rubric = state["rubric_compliance"]
        alignment = state["score_alignment"]
        overall = round(evidence * 0.4 + rubric * 0.3 + alignment * 0.3, 2)
        findings = state.get("findings", [])
        confidence = round(max(0.0, min(1.0, overall / 100.0)), 2)
        return {
            "result": ReviewScoringResult(
                overall_score=overall,
                score_alignment=alignment,
                evidence_grounding=evidence,
                rubric_compliance=rubric,
                findings=findings,
                confidence=confidence,
            )
        }


def _grading_criteria(grading: Mapping[str, Any]) -> list[ReviewScoringCriterion]:
    criteria = grading.get("criteria")
    if not isinstance(criteria, list) or not criteria:
        raise ValueError("grading.criteria must contain at least one criterion")
    try:
        return [ReviewScoringCriterion.model_validate(item) for item in criteria]
    except ValidationError as error:
        raise ValueError("grading criteria are invalid") from error


def _excerpt_is_grounded(excerpt: str, content: str) -> bool:
    normalized_excerpt = " ".join(excerpt.casefold().split())
    normalized_content = " ".join(content.split())
    return bool(normalized_excerpt) and normalized_excerpt in normalized_content


def _name(value: Any) -> str:
    return str(value or "").strip().casefold()


def _number(value: Any) -> float:
    try:
        return float(value)
    except (TypeError, ValueError) as error:
        raise ValueError("grading overall score must be numeric") from error


__all__ = ["ReviewScoringAgent", "ReviewScoringRequest", "ReviewScoringResult"]
