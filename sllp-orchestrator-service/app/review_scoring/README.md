# Review Scoring Agent

This folder contains the Review Scoring Agent. It uses LangGraph to quality-check the review and feedback that will be shown to the student.

It is independent from the Grading Agent, Evidence Agent, Review Agent, and Suggestion Agent. It does not consume another agent's output. It receives a composition, a proposed student-facing review, and optional rubric/evidence context, then decides whether that review is up to standard.

## Stages

- `validate`: validate the composition and proposed student-facing review.
- `check_review_grounding`: check that review claims are grounded in the composition.
- `check_rubric_alignment`: check that the review addresses the supplied rubric.
- `check_actionability`: check that the student receives a clear next step.
- `check_evidence`: check any evidence cited by the review.
- `assemble`: return the versioned quality-gate artifact.

## Usage

```python
from app.review_scoring import ReviewScoringAgent

result = ReviewScoringAgent().run({
    "composition": {"content": "The waves sparkled."},
    "review": {
        "findings": [{"criterion": "Description", "message": "Add more details about the setting."}],
        "evidence": [{"excerpt": "The waves sparkled"}]
    },
    "rubric": [{"criterion": "Description"}]
})
```

`result.as_artifact()` produces the camelCase contract artifact. The quality score is not the student's writing score; it is a release check for student-facing feedback.

The capability definition is available through `ReviewScoringAgent.definition()` and advertises the `review-scoring` skill under SLLP contract v1.0.
