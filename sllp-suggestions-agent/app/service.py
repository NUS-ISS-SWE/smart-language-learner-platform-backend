from __future__ import annotations

import json
import logging
import re
from dataclasses import dataclass
from re import Pattern

from app.framework import RubricFrameworkProvider, FrameworkUnavailable
from app.generator import PROMPT_VERSION, GenerationError, GenerationResult, SuggestionGenerator
from app.models import AgentUsage, RubricCriterion, Evidence, GeneratedSuggestions, Grading, Suggestion, SuggestionRequest, SuggestionResponse
from app.tools import LearnerProfileTool, ToolUnavailable


class SuggestionServiceError(RuntimeError):
    pass


# These are deliberately small, deterministic safeguards around the model's
# coaching prose. They are not a substitute for the prompt, but prevent a
# malformed response from reaching a learner when it is plainly hostile,
# contains prompt-injection language, or attempts to replace the whole work.
_UNSAFE_COACHING_PATTERNS: tuple[Pattern[str], ...] = (
    re.compile(r"\b(ignore (?:all |any |the )?(?:previous|prior) instructions?|system prompt|developer message)\b", re.IGNORECASE),
    re.compile(r"\b(as an ai|jailbreak|prompt injection)\b", re.IGNORECASE),
    re.compile(r"\b(stupid|lazy|hopeless|terrible writer|failure)\b", re.IGNORECASE),
)
_WHOLE_COMPOSITION_PATTERNS: tuple[Pattern[str], ...] = (
    re.compile(r"\b(rewrite|replace|change) (?:the )?(?:whole|entire) (?:essay|composition|piece)\b", re.IGNORECASE),
    re.compile(r"\bwrite (?:the )?(?:whole|entire) (?:essay|composition)\b", re.IGNORECASE),
)
_MAX_EXAMPLE_REVISION_CHARACTERS = 400
_MAX_EXAMPLE_REVISION_SENTENCES = 2
_MAX_GENERATION_ATTEMPTS = 2
_PRIORITIES = ("HIGH", "MEDIUM", "LOW")

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class Candidate:
    criterion: RubricCriterion
    score: float
    evidence: Evidence
    rationale: str
    weighted_gap: float


class SuggestionService:
    def __init__(self, profile_tool: LearnerProfileTool, framework_provider: RubricFrameworkProvider, generator: SuggestionGenerator):
        self.profile_tool = profile_tool
        self.framework_provider = framework_provider
        self.generator = generator

    def ready(self) -> None:
        # The production default is intentionally not ready until Admin's
        # canonical rubric endpoint is reachable. This prevents invented guidance.
        self.framework_provider.get("ENGLISH", "BEGINNER")

    def generate(self, request: SuggestionRequest) -> SuggestionResponse:
        if request.composition.language != "ENGLISH":
            raise SuggestionServiceError("this agent supports English compositions only")
        if request.review.decision != "APPROVE":
            raise SuggestionServiceError("suggestions require an approved review")
        try:
            learner = self.profile_tool.get(request.composition.user_id)
        except (ToolUnavailable, FrameworkUnavailable) as error:
            raise SuggestionServiceError(str(error)) from error
        # Evidence fetched and versioned this framework at the beginning of the workflow.
        # Reusing it makes ranking reproducible when an Admin edits a rubric while a workflow runs.
        framework = request.rubric_snapshot

        candidates = self._rank(request.grading, framework.criteria, request.composition.content)
        if not candidates:
            return SuggestionResponse(suggestionStatus="NO_PRIORITY_IMPROVEMENT", suggestions=[])
        generations: list[GenerationResult] = []
        suggestions: list[Suggestion] = []
        for batch_start in range(0, len(candidates), 3):
            if len(suggestions) >= 3:
                break
            batch = candidates[batch_start:batch_start + 3]
            try:
                accepted = self._generate_valid_suggestions(
                    self._prompt(request, learner.difficulty_level, framework.version, batch), batch, generations,
                )
            except SuggestionServiceError:
                continue
            suggestions.extend(accepted[:3 - len(suggestions)])
        if not suggestions:
            raise SuggestionServiceError("suggestion generation is temporarily unavailable")
        suggestions = [item.model_copy(update={"priority": _PRIORITIES[index]}) for index, item in enumerate(suggestions)]
        usage = _combined_usage(generations)
        return SuggestionResponse(
            suggestions=suggestions,
            modelVersion=generations[0].model_version,
            promptVersion=PROMPT_VERSION,
            tokenUsage=usage.total_tokens if usage else sum(item.token_usage or 0 for item in generations) or None,
            usage=usage,
        )

    def _generate_valid_suggestions(
        self,
        prompt: str,
        candidates: list[Candidate],
        generations: list[GenerationResult],
    ) -> list[Suggestion]:
        last_error: Exception | None = None
        # Unsupported examples are dropped; a later candidate may replace them.
        # A single retry handles malformed or unsafe responses.
        for _ in range(_MAX_GENERATION_ATTEMPTS):
            try:
                generated = self.generator.generate(prompt)
                generations.append(generated)
                suggestions = self._validate_and_attach(generated.output, candidates)
                return suggestions
            except (GenerationError, SuggestionServiceError) as error:
                last_error = error
                LOGGER.warning("Suggestion example generation attempt failed: %s", error)
        raise SuggestionServiceError("suggestion generation is temporarily unavailable") from last_error

    @staticmethod
    def _rank(grading: Grading, criteria: list[RubricCriterion], composition_content: str) -> list[Candidate]:
        assessments = {item.criterion.casefold(): item for item in grading.criteria}
        candidates: list[Candidate] = []
        has_nonperfect_assessment = False
        has_incomplete_assessment = False
        for criterion in criteria:
            assessment = assessments.get(criterion.name.casefold())
            if assessment is None:
                has_incomplete_assessment = True
                continue
            if assessment.score >= 100:
                continue
            has_nonperfect_assessment = True
            evidence = _grounded_evidence(assessment.evidence, composition_content)
            if evidence is None:
                continue
            candidates.append(Candidate(
                criterion=criterion,
                score=assessment.score,
                evidence=evidence,
                rationale=assessment.rationale,
                weighted_gap=(100 - assessment.score) * criterion.weight / 100,
            ))
        if not candidates and (has_nonperfect_assessment or has_incomplete_assessment):
            raise SuggestionServiceError("approved grading has no rubric evidence that can ground a suggestion")
        return sorted(candidates, key=lambda item: (-item.weighted_gap, item.criterion.name))

    @staticmethod
    def _prompt(request: SuggestionRequest, difficulty: str, framework_version: str, candidates: list[Candidate]) -> str:
        prompt_data = {
            "task": "Return one suggestion for each supplied candidate criterion. Do not add criteria.",
            "learnerDifficulty": difficulty,
            "frameworkVersion": framework_version,
            "composition": {"title": request.composition.title, "content": request.composition.content},
            "meaningPreservation": (
                "Revise only the cited source excerpt, using the full composition to check context. "
                "Keep the writer's stated facts and meaning. Do not turn clues into asserted facts: "
                "a result, cause, motive, feeling, or event that the writer has not stated must remain unknown. "
                "Do not add a new thought, emotion, intention, sensation, sound, person, or event to the "
                "exampleRevision. Make each recommendation specific to the cited sentence and criterion; "
                "name the phrase or missing detail to work on rather than giving generic advice. "
                "An exampleRevision is optional: give one only when a short edit actually demonstrates the "
                "recommended improvement. You may remove redundancy or improve grammar using words "
                "already present in the composition; a word-order change alone is not a word-choice or ideas fix. "
                "For missing personal details or feelings, you may give a short editable template with "
                "one bracketed blank such as [your feeling], [your thought], or [what happened]. Keep all "
                "other statements grounded in the composition. For example, 'I had practised for weeks.' "
                "may become 'I had practised for weeks. When the match ended, I felt [your feeling].' if "
                "the composition says the match ended. Tell the learner in learningTip to replace the "
                "bracketed blank with their own experience. If no useful direct edit or template is possible, "
                "set exampleRevision to null. A concrete hypothetical such as 'If you felt nervous, you "
                "could say so' belongs in the learningTip, not in text added to the learner's draft. "
                "Never present a hypothetical feeling or event as something the writer experienced."
            ),
            "candidates": [
                {
                    "criterion": item.criterion.name,
                    "guidance": item.criterion.guidance,
                    "score": item.score,
                    "gradingRationale": item.rationale,
                    "sourceExcerpt": item.evidence.model_dump(by_alias=True),
                }
                for item in candidates
            ],
            "responseSchema": {
                "suggestions": [
                    {"criterion": "exact candidate criterion", "recommendation": "specific actionable coaching", "exampleRevision": "short relevant edit or editable [your feeling] template; null if neither is possible", "learningTip": "specific practice tip; explain any bracketed blank"}
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
        candidate_positions = {item.criterion.name.casefold(): index for index, item in enumerate(candidates)}
        for output in generated.suggestions:
            key = output.criterion.casefold()
            candidate = by_name.get(key)
            if candidate is None or key in seen:
                raise SuggestionServiceError("model returned a suggestion outside the selected rubric criteria")
            seen.add(key)
            recommendation = output.recommendation.strip()
            example_revision = output.example_revision.strip() if output.example_revision else None
            learning_tip = output.learning_tip.strip() if output.learning_tip else None
            try:
                _validate_coaching_text(recommendation, "recommendation")
                if learning_tip:
                    _validate_coaching_text(learning_tip, "learning tip")
                if example_revision:
                    _validate_example_revision(example_revision, candidate.evidence.excerpt)
            except SuggestionServiceError:
                LOGGER.warning("Omitting suggestion whose coaching or example failed validation")
                continue
            suggestions.append(Suggestion(
                criterion=candidate.criterion.name,
                priority=_PRIORITIES[candidate_positions[key]],
                recommendation=recommendation,
                exampleRevision=example_revision,
                learningTip=learning_tip,
                supportingEvidence=candidate.evidence.model_dump(by_alias=True),
            ))
        return sorted(suggestions, key=lambda item: _PRIORITIES.index(item.priority))


def _combined_usage(generations: list[GenerationResult]) -> AgentUsage | None:
    usages = [item.usage for item in generations if item.usage is not None]
    if not usages:
        return None
    if len(usages) != len(generations):
        raise SuggestionServiceError("provider usage is incomplete")
    first = usages[0]
    return AgentUsage(
        provider=first.provider, model=first.model,
        promptTokens=sum(item.prompt_tokens for item in usages),
        cachedInputTokens=sum(item.cached_input_tokens for item in usages),
        completionTokens=sum(item.completion_tokens for item in usages),
        totalTokens=sum(item.total_tokens for item in usages),
        pricingVersion=first.pricing_version,
        inputPriceMicrosPerMillion=first.input_price_micros_per_million,
        cachedInputPriceMicrosPerMillion=first.cached_input_price_micros_per_million,
        outputPriceMicrosPerMillion=first.output_price_micros_per_million,
    )


def _validate_coaching_text(value: str, field: str) -> None:
    """Reject plainly unsafe or instruction-like model text before persistence."""
    if not value:
        raise SuggestionServiceError(f"model returned an empty {field}")
    if any(pattern.search(value) for pattern in _UNSAFE_COACHING_PATTERNS):
        raise SuggestionServiceError(f"model returned unsuitable {field} content")


def _validate_example_revision(value: str, source_excerpt: str) -> None:
    """Keep examples illustrative, local revisions rather than replacement work."""
    _validate_coaching_text(value, "example revision")
    if len(value) > _MAX_EXAMPLE_REVISION_CHARACTERS:
        raise SuggestionServiceError("model returned an example revision that is too long")
    sentence_count = len(re.findall(r"[.!?](?:\s|$)", value))
    if sentence_count > _MAX_EXAMPLE_REVISION_SENTENCES:
        raise SuggestionServiceError("model returned an example revision that is not a short local revision")
    if any(pattern.search(value) for pattern in _WHOLE_COMPOSITION_PATTERNS):
        raise SuggestionServiceError("model returned an example revision that does not preserve student intent")
    if _normalise_for_comparison(value) == _normalise_for_comparison(source_excerpt):
        raise SuggestionServiceError("model returned an example revision that does not improve the cited text")


def _normalise_for_comparison(value: str) -> str:
    return re.sub(r"[^\w]+", "", value, flags=re.UNICODE).casefold()


def _grounded_evidence(evidence_items: list[Evidence], composition_content: str) -> Evidence | None:
    return next((item for item in evidence_items if item.excerpt in composition_content), None)
