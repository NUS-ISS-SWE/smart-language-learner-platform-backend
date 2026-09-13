import pytest

from app.review_scoring import ReviewScoringAgent


@pytest.fixture
def agent() -> ReviewScoringAgent:
    return ReviewScoringAgent()


def review() -> dict:
    return {
        "findings": [{"criterion": "Description", "message": "Add more details about the setting."}],
        "evidence": [{"excerpt": "The blue sea was calm.", "location": "paragraph 1"}],
    }


def test_student_facing_review_receives_a_high_quality_score(agent: ReviewScoringAgent):
    result = agent.run(
        {
            "composition": {"content": "The blue sea was calm."},
            "review": review(),
            "rubric": [{"criterion": "Description"}],
        }
    )

    assert result.overall_score == 100
    assert result.evidence_grounding == 100
    assert result.rubric_compliance == 100
    assert result.review_grounding == 100
    assert result.rubric_alignment == 100
    assert result.actionability == 100
    assert result.findings == []
    assert result.model_version == "review-scoring-deterministic-v1"
    assert agent.definition()["skills"][0]["id"] == "review-scoring"
    assert result.as_artifact()["overallScore"] == 100


def test_ungrounded_review_and_missing_rubric_coverage_create_findings(agent: ReviewScoringAgent):
    result = agent.run(
        {
            "composition": {"content": "The room was quiet."},
            "review": {"findings": [{"criterion": "Description", "message": "The dragon scene is vivid."}], "evidence": [{"excerpt": "A dragon crossed the mountain."}]},
            "rubric": [{"criterion": "Description"}, {"criterion": "Emotion"}],
        }
    )

    assert result.evidence_grounding == 0
    assert result.rubric_compliance == 50
    assert result.review_grounding == 0
    assert result.rubric_alignment == 0
    assert {finding.criterion for finding in result.findings} == {"review", "rubric"}
    assert all(finding.severity == "HIGH" for finding in result.findings)


def test_invalid_composition_is_rejected_before_graph_scoring(agent: ReviewScoringAgent):
    with pytest.raises(ValueError, match="invalid review scoring request"):
        agent.run({"composition": {"content": ""}, "review": review()})


def test_agent_does_not_require_grading_output(agent: ReviewScoringAgent):
    result = agent.run({"composition": {"content": "The blue sea was calm."}, "review": review()})

    assert result.overall_score == 100
