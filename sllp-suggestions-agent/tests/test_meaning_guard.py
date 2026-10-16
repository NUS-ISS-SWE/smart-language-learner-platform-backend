from __future__ import annotations

import json

import httpx
from app.generator import OpenAIChatGenerator, _reorder_excerpt, _revision_uses_supported_words


PROMPT = json.dumps({
    "composition": {"content": "The match ended. My hands shook as I opened the door."},
    "candidates": [{"criterion": "Emotion", "sourceExcerpt": {"excerpt": "My hands shook as I opened the door."}}],
})


def test_new_descriptive_facts_and_changed_negation_are_filtered():
    assert not _revision_uses_supported_words(
        "We played in the warm, soft sand.", "We played in the sand.", "We played in the sand.",
    )
    assert not _revision_uses_supported_words(
        "I spoke.", "I did not speak.", "I did not speak.",
    )
    assert _revision_uses_supported_words(
        "In the sand, we played.", "We played in the sand.", "We played in the sand.",
    )


def _chat(content: dict, prompt_tokens: int = 10, completion_tokens: int = 5) -> httpx.Response:
    return httpx.Response(200, json={
        "model": "test-model",
        "choices": [{"message": {"content": json.dumps(content)}}],
        "usage": {"prompt_tokens": prompt_tokens, "completion_tokens": completion_tokens},
    })


def test_unsupported_feeling_is_repaired_and_checked_again():
    replies = iter([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Describe how you felt.", "exampleRevision": "My hands shook with disappointment."}]}),
        _chat({"checks": [{"criterion": "Emotion", "grounded": False, "safeRevision": "As I opened the door, my hands shook."}]}),
        _chat({"checks": [{"criterion": "Emotion", "grounded": True, "safeRevision": ""}]}),
        httpx.Response(200, json={"results": [{"flagged": False, "category_scores": {"violence": 0.01}}]}),
    ])

    def respond(request: httpx.Request) -> httpx.Response:
        return next(replies)

    with httpx.Client(transport=httpx.MockTransport(respond)) as client:
        result = OpenAIChatGenerator("test-key", "http://openai.test/v1", "test-model", 0.2, client).generate(PROMPT)

    assert result.output.suggestions[0].example_revision == "As I opened the door, my hands shook."
    assert result.usage is not None
    assert result.usage.total_tokens == 45


def test_unresolved_unsupported_example_uses_safe_local_reorder():
    replies = iter([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Describe how you felt.", "exampleRevision": "My hands shook with disappointment."}]}),
        _chat({"checks": [{"criterion": "Emotion", "grounded": False, "safeRevision": ""}]}),
        httpx.Response(200, json={"results": [{"flagged": False, "category_scores": {"violence": 0.01}}]}),
    ])
    with httpx.Client(transport=httpx.MockTransport(lambda request: next(replies))) as client:
        result = OpenAIChatGenerator("test-key", "http://openai.test/v1", "test-model", 0.2, client).generate(PROMPT)
    assert result.output.suggestions[0].recommendation == "Describe how you felt."
    assert result.output.suggestions[0].example_revision == "As I opened the door, my hands shook."


def test_reviewer_criterion_matching_ignores_case():
    replies = iter([
        _chat({"suggestions": [{"criterion": "emotion", "recommendation": "Describe how you felt.",
                                 "exampleRevision": "As I opened the door, my hands shook."}]}),
        _chat({"checks": [{"criterion": "Emotion", "grounded": True, "safeRevision": ""}]}),
        httpx.Response(200, json={"results": [{"flagged": False, "category_scores": {"violence": 0.01}}]}),
    ])
    with httpx.Client(transport=httpx.MockTransport(lambda request: next(replies))) as client:
        result = OpenAIChatGenerator("test-key", "http://openai.test/v1", "test-model", 0.2, client).generate(PROMPT)
    assert result.output.suggestions[0].example_revision == "As I opened the door, my hands shook."


def test_malformed_review_uses_local_fallback_and_counts_review_tokens():
    replies = iter([
        _chat({"suggestions": [{"criterion": "Emotion", "recommendation": "Describe how you felt.",
                                 "exampleRevision": "My hands shook with disappointment."}]}),
        _chat({"checks": [None]}),
        httpx.Response(200, json={"results": [{"flagged": False, "category_scores": {"violence": 0.01}}]}),
    ])
    with httpx.Client(transport=httpx.MockTransport(lambda request: next(replies))) as client:
        result = OpenAIChatGenerator("test-key", "http://openai.test/v1", "test-model", 0.2, client).generate(PROMPT)
    assert result.output.suggestions[0].example_revision == "As I opened the door, my hands shook."
    assert result.usage is not None
    assert result.usage.total_tokens == 30


def test_local_reorder_does_not_invent_content():
    pairs = {
        "We played in the sand.": "In the sand, we played.",
        "I had practised for weeks.": "For weeks, I had practised.",
        "He smiled when I climbed aboard.": "When I climbed aboard, he smiled.",
        "John smiled when I climbed aboard.": "When I climbed aboard, John smiled.",
        "After lunch, a girl asked for a story about the ocean.": "A girl asked for a story about the ocean after lunch.",
    }
    for source, expected in pairs.items():
        assert _reorder_excerpt(source) == expected
        assert _revision_uses_supported_words(expected, source, source)
    assert _reorder_excerpt("The room was quiet.") is None
    assert _reorder_excerpt("I was tired when I left, and I kept thinking about the girl.") is None
