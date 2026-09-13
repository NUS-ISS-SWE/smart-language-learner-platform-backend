import pytest

from app.review_scoring import ReviewScoringAgent


@pytest.fixture
def agent() -> ReviewScoringAgent:
    return ReviewScoringAgent()


def grading() -> dict:
    return {
        "overallScore": 80,
        "criteria": [
            {
                "criterion": "Description",
                "score": 80,
                "rationale": "The setting is clear.",
                "evidence": [{"excerpt": "The blue sea was calm.", "location": "paragraph 1"}],
            }
        ],
    }


def test_strong_grading_receives_a_high_audit_score(agent: ReviewScoringAgent):
    result = agent.run(
        {
            "composition": {"content": "The blue sea was calm."},
            "grading": grading(),
            "rubric": [{"criterion": "Description"}],
        }
    )

    assert result.overall_score == 100
    assert result.evidence_grounding == 100
    assert result.rubric_compliance == 100
    assert result.score_alignment == 100
    assert result.findings == []
    assert result.model_version == "review-scoring-deterministic-v1"
    assert agent.definition()["skills"][0]["id"] == "review-scoring"
    assert result.as_artifact()["overallScore"] == 100


def test_ungrounded_evidence_and_score_drift_create_high_severity_findings(agent: ReviewScoringAgent):
    result = agent.run(
        {
            "composition": {"content": "The room was quiet."},
            "grading": {
                **grading(),
                "overallScore": 40,
                "criteria": [
                    {
                        "criterion": "Description",
                        "score": 90,
                        "rationale": "The scene is vivid.",
                        "evidence": [{"excerpt": "A dragon crossed the mountain.", "location": "paragraph 4"}],
                    }
                ],
            },
            "rubric": [{"criterion": "Description"}, {"criterion": "Emotion"}],
        }
    )

    assert result.evidence_grounding == 0
    assert result.rubric_compliance == 50
    assert result.score_alignment == 0
    assert {finding.criterion for finding in result.findings} == {"evidence", "rubric", "score"}
    assert all(finding.severity == "HIGH" for finding in result.findings)


def test_invalid_composition_is_rejected_before_graph_scoring(agent: ReviewScoringAgent):
    with pytest.raises(ValueError, match="invalid review scoring request"):
        agent.run({"composition": {"content": ""}, "grading": grading()})
