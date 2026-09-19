from uuid import UUID, uuid4

import pytest

from app.models import AgentUsage, WorkflowSubmission
from app.worker import KafkaWorkflowWorker


def test_legacy_grading_event_is_normalized_to_v1_contract():
    submission = WorkflowSubmission.from_kafka_payload(
        {
            "compositionId": 12,
            "userId": 8,
            "title": "My first trip",
            "content": "I went to the beach.",
            "language": "ENGLISH",
            "requestedAt": "2026-08-29T09:00:00Z",
        }
    )

    request = submission.request_payload(uuid4(), 1)
    assert submission.composition.composition_id == 12
    assert request["composition"]["compositionId"] == 12
    assert request["schemaVersion"] == "1.0"


def test_current_grading_event_retains_request_identity_for_the_completion_contract():
    request_id = UUID("9b6efdf7-9ed7-4dcc-b1e5-d617b9c644a0")
    submission = WorkflowSubmission.from_kafka_payload(
        {
            "requestId": str(request_id),
            "compositionId": 12,
            "userId": 8,
            "title": "My first trip",
            "content": "I went to the beach.",
            "language": "ENGLISH",
            "requestedAt": "2026-08-29T09:00:00Z",
        }
    )

    assert submission.request_id == request_id
    assert submission.idempotency_key("topic:0:1") == str(request_id)
    assert submission.completion_request_id(uuid4()) == request_id


def test_legacy_completion_identity_is_unique_per_workflow_not_submission_content():
    submission = WorkflowSubmission.from_kafka_payload(
        {
            "compositionId": 12,
            "userId": 8,
            "title": "My first trip",
            "content": "I went to the beach.",
            "language": "ENGLISH",
        }
    )

    first_workflow = UUID("9b6efdf7-9ed7-4dcc-b1e5-d617b9c644a0")
    second_workflow = UUID("b0b2f06d-7b1c-4c80-8b1c-ff8b45f39753")
    assert submission.idempotency_key("topic:0:1") == "topic:0:1"
    assert submission.completion_request_id(first_workflow) != submission.completion_request_id(second_workflow)


def test_versioned_envelope_is_normalized():
    submission = WorkflowSubmission.from_kafka_payload(
        {
            "schemaVersion": "1.0",
            "eventId": "event-123",
            "correlationId": "corr-123",
            "data": {
                "composition": {
                    "compositionId": 12,
                    "userId": 8,
                    "title": "Title",
                    "content": "Content",
                    "language": "ENGLISH",
                }
            },
        }
    )

    assert submission.event_id == "event-123"
    assert submission.correlation_id == "corr-123"
    assert submission.composition.title == "Title"


def test_worker_decodes_only_valid_json_object_requests():
    submission = KafkaWorkflowWorker._decode_submission(
        b'{"compositionId":12,"userId":8,"title":"Title","content":"Content","language":"ENGLISH"}'
    )

    assert submission.composition.composition_id == 12
    with pytest.raises(ValueError):
        KafkaWorkflowWorker._decode_submission(b"[]")


def test_agent_usage_rejects_an_impossible_cached_or_total_token_breakdown():
    valid = {
        "provider": "provider",
        "model": "model",
        "promptTokens": 100,
        "cachedInputTokens": 20,
        "completionTokens": 30,
        "totalTokens": 130,
        "pricingVersion": "catalog-v1",
        "inputPriceMicrosPerMillion": 1,
        "cachedInputPriceMicrosPerMillion": 1,
        "outputPriceMicrosPerMillion": 1,
    }

    assert AgentUsage.model_validate(valid).total_tokens == 130
    with pytest.raises(ValueError):
        AgentUsage.model_validate({**valid, "cachedInputTokens": 101})
    with pytest.raises(ValueError):
        AgentUsage.model_validate({**valid, "totalTokens": 129})
    with pytest.raises(ValueError):
        AgentUsage.model_validate({**valid, "totalTokens": 131})
