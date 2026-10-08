from app.evaluation_scorecard import score_case


def _case():
    return {"id": "case-1", "content": "I walked to the blue sea and felt calm.", "expectedCriteria": ["Description", "Emotion"]}


def _result(excerpt="blue sea"):
    return {
        "grading": {"overallScore": 72, "criteria": [
            {"criterion": "Description", "evidence": [{"excerpt": excerpt}]},
            {"criterion": "Emotion", "evidence": [{"excerpt": "felt calm"}]},
        ]},
        "review": {"decision": "APPROVE"},
        "suggestions": {"suggestions": [{"recommendation": "Add one sensory detail."}]},
        "usage": {"provider": "test", "model": "test", "promptTokens": 10, "completionTokens": 5},
    }


def test_grounded_approved_case_scores_full_marks():
    assert score_case(_case(), _result())["score"] == 100


def test_ungrounded_evidence_loses_only_the_grounding_dimension():
    scored = score_case(_case(), _result("made up excerpt"))
    assert scored["dimensions"]["grounding"] == 0
    assert scored["score"] == 70
