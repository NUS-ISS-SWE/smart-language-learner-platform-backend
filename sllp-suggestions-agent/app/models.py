from __future__ import annotations

from typing import Any, Literal

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, model_validator


def _camel_case(value: str) -> str:
    head, *tail = value.split("_")
    return head + "".join(part.capitalize() for part in tail)


class ApiModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, alias_generator=_camel_case)


class Composition(ApiModel):
    composition_id: int = Field(validation_alias=AliasChoices("compositionId", "composition_id"))
    user_id: int = Field(validation_alias=AliasChoices("userId", "user_id"))
    title: str = Field(min_length=1, max_length=500)
    content: str = Field(min_length=1, max_length=20_000)
    language: Literal["ENGLISH", "CHINESE"]


class Evidence(ApiModel):
    excerpt: str = Field(min_length=1, max_length=2_000)
    location: str | None = Field(default=None, max_length=200)


class CriterionAssessment(ApiModel):
    criterion: str = Field(min_length=1, max_length=200)
    score: float = Field(ge=0, le=100)
    rationale: str = Field(min_length=1, max_length=4_000)
    evidence: list[Evidence] = Field(min_length=1)


class Grading(ApiModel):
    rubric_scores: dict[str, float] = Field(default_factory=dict, validation_alias=AliasChoices("rubricScores", "rubric_scores", "deepScores", "deep_scores"))
    criteria: list[CriterionAssessment] = Field(min_length=1)


class Review(ApiModel):
    decision: Literal["APPROVE", "REGRADE", "REJECT"]
    findings: list[dict[str, Any]] = Field(default_factory=list)


class SuggestionRequest(ApiModel):
    schema_version: str = Field(validation_alias=AliasChoices("schemaVersion", "schema_version"))
    workflow_id: str = Field(validation_alias=AliasChoices("workflowId", "workflow_id"))
    attempt: int = Field(ge=1)
    composition: Composition
    grading: Grading
    review: Review
    rubric_snapshot: "RubricFramework" = Field(validation_alias=AliasChoices("rubricSnapshot", "rubric_snapshot"))


class SupportingEvidence(Evidence):
    pass


class Suggestion(ApiModel):
    criterion: str = Field(min_length=1, max_length=200)
    priority: Literal["HIGH", "MEDIUM", "LOW"]
    recommendation: str = Field(min_length=1, max_length=1_200)
    example_revision: str | None = Field(default=None, max_length=1_200)
    learning_tip: str | None = Field(default=None, max_length=1_200)
    supporting_evidence: SupportingEvidence = Field(validation_alias=AliasChoices("supportingEvidence", "supporting_evidence"))


class SuggestionResponse(ApiModel):
    suggestion_status: Literal["AVAILABLE", "NOT_AVAILABLE_FOR_LANGUAGE"] = Field(
        default="AVAILABLE", validation_alias=AliasChoices("suggestionStatus", "suggestion_status")
    )
    suggestions: list[Suggestion] = Field(default_factory=list, max_length=3)
    model_version: str | None = Field(default=None, validation_alias=AliasChoices("modelVersion", "model_version"))
    prompt_version: str | None = Field(default=None, validation_alias=AliasChoices("promptVersion", "prompt_version"))
    token_usage: int | None = Field(default=None, ge=0, validation_alias=AliasChoices("tokenUsage", "token_usage"))
    usage: "AgentUsage | None" = None

    @model_validator(mode="after")
    def validate_status(self) -> "SuggestionResponse":
        if self.suggestion_status == "AVAILABLE" and not self.suggestions:
            raise ValueError("available suggestions require at least one item")
        if self.suggestion_status != "AVAILABLE" and self.suggestions:
            raise ValueError("unavailable suggestions must be empty")
        return self


class GeneratedSuggestion(ApiModel):
    criterion: str = Field(min_length=1, max_length=200)
    recommendation: str = Field(min_length=1, max_length=1_200)
    example_revision: str | None = Field(default=None, max_length=1_200)
    learning_tip: str | None = Field(default=None, max_length=1_200)


class GeneratedSuggestions(ApiModel):
    suggestions: list[GeneratedSuggestion] = Field(min_length=1, max_length=3)


class RubricCriterion(ApiModel):
    name: str = Field(min_length=1, max_length=200)
    weight: float = Field(gt=0, le=100)
    guidance: str = Field(min_length=1, max_length=4_000)


class RubricFramework(ApiModel):
    version: str = Field(min_length=1, max_length=100)
    criteria: list[RubricCriterion] = Field(min_length=1, max_length=10)

    @model_validator(mode="after")
    def require_valid_weights(self) -> "RubricFramework":
        if round(sum(item.weight for item in self.criteria), 6) != 100:
            raise ValueError("rubric criterion weights must total 100")
        return self


class AgentUsage(ApiModel):
    provider: str = Field(min_length=1)
    model: str = Field(min_length=1)
    prompt_tokens: int = Field(ge=0)
    cached_input_tokens: int = Field(default=0, ge=0)
    completion_tokens: int = Field(ge=0)
    total_tokens: int = Field(ge=0)
    pricing_version: str = Field(min_length=1)
    input_price_micros_per_million: int = Field(ge=0)
    cached_input_price_micros_per_million: int = Field(ge=0)
    output_price_micros_per_million: int = Field(ge=0)

    @model_validator(mode="after")
    def valid_usage(self) -> "AgentUsage":
        if self.cached_input_tokens > self.prompt_tokens or self.total_tokens != self.prompt_tokens + self.completion_tokens:
            raise ValueError("invalid provider token usage")
        return self


class LearnerProfile(ApiModel):
    id: int
    difficulty_level: str = Field(validation_alias=AliasChoices("difficultyLevel", "difficulty_level"))


# Compatibility aliases for existing fixture-only tests; production uses rubric terminology.
DeepCriterion = RubricCriterion
DeepFramework = RubricFramework
