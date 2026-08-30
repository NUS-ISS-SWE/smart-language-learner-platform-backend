from uuid import uuid4

import pytest

from app.models import WorkflowSubmission
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
