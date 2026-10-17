from __future__ import annotations

import json

import httpx

from app.generator import OpenAIChatGenerator, _revision_passes_guards, _template_for_criterion


FEELING_PROMPT = json.dumps({
    "composition": {"content": "The match ended. My hands shook as I opened the door."},
    "candidates": [{"criterion": "Emotion", "sourceExcerpt": {"excerpt": "My hands shook as I opened the door."}}],
})
WORD_CHOICE_PROMPT = json.dumps({
    "composition": {"content": "The referee blew the final whistle at the end of the match."},
    "candidates": [{"criterion": "Word choice", "sourceExcerpt": {
        "excerpt": "The referee blew the final whistle at the end of the match.",
    }}],
})


def _chat(content: dict, prompt_tokens: int = 10, completion_tokens: int = 5) -> httpx.Response:
    return httpx.Response(200, json={
        "model": "test-model",
        "choices": [{"message": {"content": json.dumps(content)}}],
        "usage": {"prompt_tokens": prompt_tokens, "completion_tokens": completion_tokens},
    })


def _moderation() -> httpx.Response:
    return httpx.Response(200, json={"results": [{"flagged": False, "category_scores": {"violence": 0.01}}]})


def _generator(replies: list[httpx.Response]) -> OpenAIChatGenerator:
    responses = iter(replies)
    client = httpx.Client(transport=httpx.MockTransport(lambda request: next(responses)))
    return OpenAIChatGenerator("test-key", "http://openai.test/v1", "test-model", 0.2, client)


def test_guard_allows_concision_but_blocks_new_descriptive_facts_and_negation():
    source = "The referee blew the final whistle at the end of the match."
    assert _revision_passes_guards("The referee blew the final whistle.", source, source)
    assert not _revision_passes_guards("The referee sounded the final whistle.", source, source)
    assert not _revision_passes_guards("I felt nervous when the referee sounded the final whistle.", source, source)
    assert not _revision_passes_guards("I scored when the referee blew the final whistle.", source, source)
    assert not _revision_passes_guards("The referee did not blow the final whistle.", source, source)
    assert not _revision_passes_guards(
        "We played in the warm, soft sand.", "We played in the sand.", "We played in the sand."
    )
    feeling_source = "My hands shook as I opened the door."
    feeling_composition = "The match ended. My hands shook as I opened the door."
    assert _revision_passes_guards(
        "My hands shook as I opened the door. After the match, I felt [your feeling].",
        feeling_source, feeling_composition,
    )
    assert not _revision_passes_guards(
        "My hands shook as I opened the door. After the match, I felt [nervous].",
        feeling_source, feeling_composition,
    )


def test_unsupported_feeling_is_replaced_by_an_editable_blank():
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Describe how you felt.",
                               "exampleRevision": "My hands shook with disappointment.",
                               "learningTip": "If you felt nervous, say so in your own words."}]}),
        _chat({"checks": [{"criterion": "Emotion", "grounded": False, "aligned": True, "safeRevision": ""}]}),
        _moderation(),
    ])
    result = generator.generate(FEELING_PROMPT)
    assert result.output.suggestions[0].example_revision == (
        "My hands shook as I opened the door. I felt [your feeling]."
    )
    assert result.output.suggestions[0].learning_tip.startswith("Replace [your feeling]")
    assert result.usage is not None and result.usage.total_tokens == 30


def test_approved_word_choice_concision_edit_is_kept():
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Word choice", "recommendation": "Use a concise verb.",
                               "exampleRevision": "The referee blew the final whistle."}]}),
        _chat({"checks": [{"criterion": "Word choice", "grounded": True, "aligned": True,
                           "safeRevision": ""}]}),
        _moderation(),
    ])
    result = generator.generate(WORD_CHOICE_PROMPT)
    assert result.output.suggestions[0].example_revision == "The referee blew the final whistle."
    assert "remove repetition" in result.output.suggestions[0].learning_tip


def test_grounded_but_irrelevant_reorder_is_removed():
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Word choice", "recommendation": "Use a more precise word.",
                               "exampleRevision": "At the end of the match, the referee blew the final whistle."}]}),
        _chat({"checks": [{"criterion": "Word choice", "grounded": True, "aligned": False,
                           "safeRevision": ""}]}),
        _moderation(),
    ])
    assert generator.generate(WORD_CHOICE_PROMPT).output.suggestions[0].example_revision is None


def test_reviewer_false_positive_cannot_add_sensory_details():
    prompt = json.dumps({
        "composition": {"content": "We played in the sand."},
        "candidates": [{"criterion": "Description", "sourceExcerpt": {"excerpt": "We played in the sand."}}],
    })
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Description", "recommendation": "Describe the sand.",
                               "exampleRevision": "We played in the warm, soft sand."}]}),
        _chat({"checks": [{"criterion": "Description", "grounded": True, "aligned": True,
                           "safeRevision": ""}]}),
        _moderation(),
    ])
    assert generator.generate(prompt).output.suggestions[0].example_revision is None


def test_safe_repair_is_checked_again():
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Word choice", "recommendation": "Remove redundant wording.",
                               "exampleRevision": "The referee blew the loud final whistle."}]}),
        _chat({"checks": [{"criterion": "Word choice", "grounded": False, "aligned": False,
                           "safeRevision": "The referee blew the final whistle."}]}),
        _chat({"checks": [{"criterion": "Word choice", "grounded": True, "aligned": True,
                           "safeRevision": ""}]}),
        _moderation(),
    ])
    result = generator.generate(WORD_CHOICE_PROMPT)
    assert result.output.suggestions[0].example_revision == "The referee blew the final whistle."
    assert result.usage is not None and result.usage.total_tokens == 45


def test_malformed_review_uses_only_a_grounded_blank_template():
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Describe how you felt.",
                               "exampleRevision": "My hands shook with disappointment."}]}),
        _chat({"checks": [None]}),
        _moderation(),
    ])
    assert generator.generate(FEELING_PROMPT).output.suggestions[0].example_revision == (
        "My hands shook as I opened the door. I felt [your feeling]."
    )


def test_no_model_example_skips_review_and_uses_safe_template():
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Explain how you felt.",
                               "exampleRevision": None, "learningTip": "If you felt nervous, describe why."}]}),
        _moderation(),
    ])
    assert generator.generate(FEELING_PROMPT).output.suggestions[0].example_revision == (
        "My hands shook as I opened the door. I felt [your feeling]."
    )


def test_templates_are_limited_to_content_criteria_and_complete_sentences():
    assert _template_for_criterion("Reflection", "I had practised for weeks.") == (
        "I had practised for weeks. Looking back, I think [your thought]."
    )
    assert _template_for_criterion("Plot", "The bell rang at the end of the match.") == (
        "The bell rang at the end of the match. Then [what happened]."
    )
    assert _template_for_criterion(
        "Plot", "The bell rang at the end of the match.", "Describe what happened before the bell rang."
    ) == "Before that, [what happened]. The bell rang at the end of the match."
    assert _template_for_criterion(
        "Plot", "The bell rang at the end of the match.", "Add events leading up to the bell."
    ) == "Before that, [what happened]. The bell rang at the end of the match."
    assert _template_for_criterion("Word choice", "The referee blew the final whistle.") is None
    assert _template_for_criterion("Emotion", "walked home") is None
    assert _template_for_criterion("Emotion", "He smiled when I climbed aboard.") is None
    assert _template_for_criterion("Emotion", "My friend Sam smiled at me.") is None
    assert _template_for_criterion("Emotion", "I was happy.") == "I was happy. I [what you did]."
    assert _template_for_criterion("Emotion", "We were happy.") == "We were happy. We [what we did]."


def test_reviewed_editable_feeling_template_is_kept():
    template = "My hands shook as I opened the door. After the match, I felt [your feeling]."
    generator = _generator([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Name your actual feeling.",
                               "exampleRevision": template,
                               "learningTip": "Replace [your feeling] with what you actually felt."}]}),
        _chat({"checks": [{"criterion": "Emotion", "grounded": True, "aligned": True,
                           "safeRevision": ""}]}),
        _moderation(),
    ])
    assert generator.generate(FEELING_PROMPT).output.suggestions[0].example_revision == template
