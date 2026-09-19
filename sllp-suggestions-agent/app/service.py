from __future__ import annotations

import json
from dataclasses import dataclass

from app.framework import DeepFrameworkProvider, FrameworkUnavailable
from app.generator import PROMPT_VERSION, GenerationError, SuggestionGenerator
from app.models import DeepCriterion, Evidence, GeneratedSuggestions, Grading, Suggestion, SuggestionRequest, SuggestionResponse
from app.tools import LearnerProfileTool, ToolUnavailable


class SuggestionServiceError(RuntimeError):
    pass


@dataclass(frozen=True)
class Candidate:
    criterion: DeepCriterion
    score: float
    evidence: Evidence
    rationale: str
    weighted_gap: float


class SuggestionService:
    def __init__(self, profile_tool: LearnerProfileTool, framework_provider: DeepFrameworkProvider, generator: SuggestionGenerator):
        self.profile_tool = profile_tool
        self.framework_provider = framework_provider
        self.generator = generator

    def ready(self) -> None:
        # The production default is intentionally not ready until the DEEP owner
        # connects the canonical provider. This prevents invented rubric guidance.
        self.framework_provider.get("BEGINNER")

    def generate(self, request: SuggestionRequest) -> SuggestionResponse:
        if request.composition.language != "ENGLISH":
            raise SuggestionServiceError("this agent supports English compositions only")
        if request.review.decision != "APPROVE":
            raise SuggestionServiceError("suggestions require an approved review")
        try:
            learner = self.profile_tool.get(request.composition.user_id)
            framework = self.framework_provider.get(learner.difficulty_level)
        except (ToolUnavailable, FrameworkUnavailable) as error:
            raise SuggestionServiceError(str(error)) from error

        candidates = self._rank(request.grading, framework.criteria)
        try:
            generated = self.generator.generate(self._prompt(request, learner.difficulty_level, framework.version, candidates))
        except GenerationError as error:
            raise SuggestionServiceError("suggestion generation is temporarily unavailable") from error
        suggestions = self._validate_and_attach(generated.output, candidates)
        return SuggestionResponse(
            suggestions=suggestions,
            modelVersion=generated.model_version,
            promptVersion=PROMPT_VERSION,
            tokenUsage=generated.token_usage,
        )

    @staticmethod
    def _rank(grading: Grading, criteria: list[DeepCriterion]) -> list[Candidate]:
        assessments = {item.criterion.casefold(): item for item in grading.criteria}
        candidates: list[Candidate] = []
        for criterion in criteria:
            assessment = assessments.get(criterion.name.casefold())
            score = assessment.score if assessment else grading.deep_scores.get(criterion.name)
            if score is None or assessment is None or not assessment.evidence:
                continue
            candidates.append(Candidate(
                criterion=criterion,
                score=score,
                evidence=assessment.evidence[0],
                rationale=assessment.rationale,
                weighted_gap=(100 - score) * criterion.weight / 100,
            ))
        if not candidates:
            raise SuggestionServiceError("approved grading has no DEEP evidence that can ground a suggestion")
        return sorted(candidates, key=lambda item: (-item.weighted_gap, item.criterion.name))[:3]

    @staticmethod
    def _prompt(request: SuggestionRequest, difficulty: str, framework_version: str, candidates: list[Candidate]) -> str:
        prompt_data = {
            "task": "Return one suggestion for each supplied candidate criterion. Do not add criteria.",
            "learnerDifficulty": difficulty,
            "frameworkVersion": framework_version,
            "composition": {"title": request.composition.title, "content": request.composition.content},
            "candidates": [
                {
                    "criterion": item.criterion.name,
                    "guidance": item.criterion.guidance,
                    "score": item.score,
                    "gradingRationale": item.rationale,
                    "evidence": item.evidence.model_dump(by_alias=True),
                }
                for item in candidates
            ],
            "responseSchema": {
                "suggestions": [
                    {"criterion": "exact candidate criterion", "recommendation": "actionable coaching", "exampleRevision": "optional short example", "learningTip": "optional practice tip"}
                ]
            },
        }
        return json.dumps(prompt_data, ensure_ascii=False)

    @staticmethod
    def _validate_and_attach(generated: GeneratedSuggestions, candidates: list[Candidate]) -> list[Suggestion]:
        by_name = {item.criterion.name.casefold(): item for item in candidates}
        seen: set[str] = set()
        if len(generated.suggestions) != len(candidates):
            raise SuggestionServiceError("model did not return exactly one suggestion for each selected criterion")
        suggestions: list[Suggestion] = []
        priorities = ("HIGH", "MEDIUM", "LOW")
        for output in generated.suggestions:
            key = output.criterion.casefold()
            candidate = by_name.get(key)
            if candidate is None or key in seen:
                raise SuggestionServiceError("model returned a suggestion outside the selected DEEP criteria")
            seen.add(key)
            suggestions.append(Suggestion(
                criterion=candidate.criterion.name,
                priority=priorities[candidates.index(candidate)],
                recommendation=output.recommendation.strip(),
                exampleRevision=output.example_revision.strip() if output.example_revision else None,
                learningTip=output.learning_tip.strip() if output.learning_tip else None,
                supportingEvidence=candidate.evidence.model_dump(by_alias=True),
            ))
        return sorted(suggestions, key=lambda item: priorities.index(item.priority))
