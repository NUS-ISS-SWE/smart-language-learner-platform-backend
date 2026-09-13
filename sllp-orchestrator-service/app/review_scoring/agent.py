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
    """Student work and proposed student-facing review to quality-check."""

    model_config = ConfigDict(extra="allow", populate_by_name=True)

    composition: dict[str, Any]
    review: dict[str, Any]
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
    """Auditable quality score for feedback before it is shown to a student."""

    model_config = ConfigDict(populate_by_name=True, alias_generator=_camel_case)

    overall_score: float = Field(ge=0, le=100)
    review_grounding: float = Field(ge=0, le=100)
    rubric_alignment: float = Field(ge=0, le=100)
    actionability: float = Field(ge=0, le=100)
    evidence_grounding: float = Field(ge=0, le=100)
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
    review_grounding: float
    rubric_alignment: float
    actionability: float
    findings: list[ReviewScoringFinding]
    result: ReviewScoringResult
    error: str


class ReviewScoringAgent:
    """LangGraph quality gate for feedback before it reaches the student."""

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
                    "description": "Checks student-facing review grounding, rubric alignment, and actionability.",
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
        graph.add_node("check_review_grounding", self._check_review_grounding)
        graph.add_node("check_rubric_alignment", self._check_rubric_alignment)
        graph.add_node("check_actionability", self._check_actionability)
        graph.add_node("check_evidence", self._check_evidence)
        graph.add_node("assemble", self._assemble)
        graph.add_edge(START, "validate")
        graph.add_conditional_edges("validate", self._after_validate, {"check_review_grounding": "check_review_grounding", "fail": END})
        graph.add_edge("check_review_grounding", "check_rubric_alignment")
        graph.add_edge("check_rubric_alignment", "check_actionability")
        graph.add_edge("check_actionability", "check_evidence")
        graph.add_edge("check_evidence", "assemble")
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
    def _check_review_grounding(state: _ReviewScoringState) -> _ReviewScoringState:
        request = state["request"]
        content = str(request.composition["content"]).casefold()
        review_text = _review_text(request.review)
        claims = _review_claims(request.review)
        grounded = sum(_excerpt_is_grounded(claim, content) for claim in claims)
        score = round(100 * grounded / len(claims), 2) if claims else 0.0
        findings = []
        if score < 70:
            findings.append(ReviewScoringFinding(criterion="review", message="The student-facing review contains claims that are not grounded in the composition.", severity="HIGH"))
        elif score < 90:
            findings.append(ReviewScoringFinding(criterion="review", message="Some review claims are weakly grounded in the composition.", severity="MEDIUM"))
        return {"review_grounding": score, "findings": findings}

    @staticmethod
    def _check_rubric_alignment(state: _ReviewScoringState) -> _ReviewScoringState:
        request = state["request"]
        if not request.rubric:
            return {"rubric_alignment": 100.0}
        review_text = _review_text(request.review)
        matched = sum(_name(item.get("criterion") or item.get("name")) in _name(review_text) for item in request.rubric)
        score = round(100 * matched / len(request.rubric), 2)
        findings = list(state.get("findings", []))
        if score < 100:
            findings.append(ReviewScoringFinding(criterion="rubric", message="The review does not address every rubric criterion.", severity="HIGH" if score < 70 else "MEDIUM"))
        return {"rubric_alignment": score, "findings": findings}

    @staticmethod
    def _check_actionability(state: _ReviewScoringState) -> _ReviewScoringState:
        review_text = _review_text(state["request"].review)
        action_words = ("add", "explain", "describe", "use", "revise", "change", "include", "try")
        has_action = any(word in review_text.casefold().split() for word in action_words)
        score = 100.0 if has_action else 40.0
        findings = list(state.get("findings", []))
        if not has_action:
            findings.append(ReviewScoringFinding(criterion="actionability", message="The review does not give the student a clear next step.", severity="MEDIUM"))
        return {"actionability": score, "findings": findings}

    @staticmethod
    def _check_evidence(state: _ReviewScoringState) -> _ReviewScoringState:
        request = state["request"]
        content = str(request.composition["content"]).casefold()
        evidence = request.evidence
        if not evidence:
            return {"evidence_grounding": 100.0}

        grounded = sum(_excerpt_is_grounded(item.excerpt, content) for item in evidence)
        score = round(100 * grounded / len(evidence), 2)
        findings = []
        if score < 70:
            findings.append(ReviewScoringFinding(criterion="evidence", message="Some cited review evidence cannot be located in the composition.", severity="HIGH"))
        elif score < 90:
            findings.append(ReviewScoringFinding(criterion="evidence", message="Some cited review evidence is weakly grounded in the composition.", severity="MEDIUM"))
        return {"evidence_grounding": score, "findings": findings}

    @staticmethod
    def _assemble(state: _ReviewScoringState) -> _ReviewScoringState:
        evidence = state["evidence_grounding"]
        grounding = state["review_grounding"]
        rubric = state["rubric_alignment"]
        actionability = state["actionability"]
        overall = round(grounding * 0.35 + rubric * 0.25 + actionability * 0.25 + evidence * 0.15, 2)
        findings = state.get("findings", [])
        confidence = round(max(0.0, min(1.0, overall / 100.0)), 2)
        return {
            "result": ReviewScoringResult(
                overall_score=overall,
                review_grounding=grounding,
                rubric_alignment=rubric,
                actionability=actionability,
                evidence_grounding=evidence,
                findings=findings,
                confidence=confidence,
            )
        }


def _review_text(review: Mapping[str, Any]) -> str:
    parts = [review.get("message"), review.get("feedback"), review.get("recommendation")]
    for item in review.get("findings", []) if isinstance(review.get("findings"), list) else []:
        if isinstance(item, Mapping):
            parts.append(item.get("message"))
    return " ".join(str(part) for part in parts if part)


def _review_claims(review: Mapping[str, Any]) -> list[str]:
    claims = review.get("evidence", [])
    return [str(item.get("excerpt")) for item in claims if isinstance(item, Mapping) and item.get("excerpt")]


def _excerpt_is_grounded(excerpt: str, content: str) -> bool:
    normalized_excerpt = " ".join(excerpt.casefold().split())
    normalized_content = " ".join(content.split())
    return bool(normalized_excerpt) and normalized_excerpt in normalized_content


def _name(value: Any) -> str:
    return str(value or "").strip().casefold()


__all__ = ["ReviewScoringAgent", "ReviewScoringRequest", "ReviewScoringResult"]
