# Review Scoring Agent

This folder contains the fifth SLLP assessment specialist. `ReviewScoringAgent` is a LangGraph workflow that audits grading quality before the existing Review Agent decides whether to approve, re-grade, or reject an assessment.

## Stages

- `validate`: validate the composition and grading artifact.
- `analyze_evidence`: check that grading excerpts appear in the composition.
- `check_rubric`: check coverage of supplied rubric criteria.
- `check_consistency`: compare the overall score with criterion scores.
- `assemble`: return the versioned audit artifact.

## Usage

```python
from app.review_scoring import ReviewScoringAgent

result = ReviewScoringAgent().run({
    "composition": {"content": "The waves sparkled."},
    "grading": {
        "overallScore": 78,
        "criteria": [{
            "criterion": "Description",
            "score": 78,
            "rationale": "The image is specific.",
            "evidence": [{"excerpt": "The waves sparkled"}]
        }]
    },
    "rubric": [{"criterion": "Description"}]
})
```

`result.as_artifact()` produces the camelCase contract artifact. The audit score never replaces the student's grading score.

The capability definition is available through `ReviewScoringAgent.definition()` and advertises the `review-scoring` skill under SLLP contract v1.0.
