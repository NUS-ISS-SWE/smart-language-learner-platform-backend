from __future__ import annotations

from typing import Any
from uuid import uuid4

from app.agent_types import AgentAttempt, AgentInvocation, T
from app.provider_models import AgentName


class PlaceholderAgentProvider:
    """Contract-valid development responses while team-owned agents are unavailable.

    This provider deliberately makes no model or network call.  Its output is
    labelled as a placeholder in both metadata and the student-facing report,
    so it cannot be mistaken for a real assessment.
    """

    name = "placeholder"

    def ensure_ready(self, include_suggestions: bool = True) -> dict[str, str]:
        return {"mode": "development-placeholder", "version": "placeholder-v1"}

    def invoke(self, agent: AgentName, payload: dict[str, Any], parser: type[T]) -> AgentInvocation[T]:
        return AgentInvocation(
            parser.model_validate(_placeholder_response(agent)),
            (AgentAttempt(1, "SUCCEEDED", 0, 200),),
            f"placeholder-{uuid4()}",
            payload["workflowId"],
            self.name,
        )


def _placeholder_response(agent: AgentName) -> dict[str, Any]:
    # The placeholder makes no provider call, so its recorded usage is an explicit,
    # auditable zero rather than an absent ledger event.
    metadata = {
        "modelVersion": "placeholder-v1",
        "promptVersion": "placeholder-v1",
        "tokenUsage": 0,
        "usage": {
            "provider": "placeholder",
            "model": "placeholder-v1",
            "promptTokens": 0,
            "cachedInputTokens": 0,
            "completionTokens": 0,
            "totalTokens": 0,
            "pricingVersion": "placeholder-v1",
            "inputPriceMicrosPerMillion": 0,
            "cachedInputPriceMicrosPerMillion": 0,
            "outputPriceMicrosPerMillion": 0,
        },
    }
    if agent == "grading":
        return {
            "overallScore": 80,
            "rubricScores": {"Development placeholder": 80},
            "rubricVersion": "placeholder-v1",
            "criteria": [
                {
                    "criterion": "Development placeholder",
                    "bandScore": 4,
                    "score": 80,
                    "rationale": "This is a development placeholder response, not an AI-generated assessment.",
                    "evidence": [{"excerpt": "Submitted composition", "location": "development mode"}],
                }
            ],
            "confidence": 0.99,
            **metadata,
        }
    if agent == "evidence":
        return {
            "plan": ["Development-only evidence placeholder."],
            "rubricVersion": "placeholder-v1",
            "rubricSnapshot": {"version": "placeholder-v1", "criteria": [{"name": "Development placeholder", "weight": 100, "guidance": "Development-only rubric."}]},
            "criteria": [{"criterion": "Development placeholder", "evidence": [{"excerpt": "Submitted composition", "polarity": "STRENGTH", "note": "Development-only evidence.", "verifiedOnFirstPass": True}]}],
            **metadata,
        }
    if agent == "review":
        return {
            "decision": "APPROVE",
            "confidence": 0.99,
            "findings": [{"message": "Development placeholder response approved for workflow testing."}],
            **metadata,
        }
    return {
        "suggestions": [
            {
                "criterion": "Development placeholder",
                "priority": "HIGH",
                "recommendation": "Replace this placeholder provider with the team-owned assessment agents before release.",
                "exampleRevision": "Use the actual grading, review, and suggestion agent outputs here.",
                "learningTip": "This result verifies the integration flow only; it is not writing advice.",
                "supportingEvidence": {"excerpt": "Submitted composition", "location": "development mode"},
            }
        ],
        **metadata,
    }
