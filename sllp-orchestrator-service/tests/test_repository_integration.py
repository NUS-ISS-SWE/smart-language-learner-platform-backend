"""PostgreSQL recovery checks; opt in with SLLP_TEST_DATABASE_URL."""

import os
from datetime import datetime, timedelta, timezone
from uuid import uuid4

import pytest
from sqlalchemy import create_engine, select, update

from app.models import OutboxEvent, WorkflowRecord, WorkflowStatus
from app.repository import LeaseLostError, WorkflowRepository, metadata, workflow_outbox, workflow_run


@pytest.fixture
def repository():
    url = os.getenv("SLLP_TEST_DATABASE_URL")
    if not url:
        pytest.skip("SLLP_TEST_DATABASE_URL is not configured")
    base = create_engine(url)
    schema = f"review_{uuid4().hex}"
    with base.begin() as connection:
        connection.exec_driver_sql(f'CREATE SCHEMA "{schema}"')
    engine = base.execution_options(schema_translate_map={None: schema})
    metadata.create_all(engine)
    try:
        yield WorkflowRepository(url, engine=engine)
    finally:
        with base.begin() as connection:
            connection.exec_driver_sql(f'DROP SCHEMA "{schema}" CASCADE')
        base.dispose()


def add_workflow(repository):
    record = WorkflowRecord(source_event_id=str(uuid4()), input_payload={"composition": {
        "compositionId": 1, "userId": 2, "title": "Test", "content": "My writing.", "language": "ENGLISH",
    }})
    return repository.create_or_get(record)[0]


def test_duplicate_intake_and_expired_worker_are_fenced(repository):
    record = add_workflow(repository)
    duplicate, created = repository.create_or_get(record)
    assert not created and duplicate.workflow_id == record.workflow_id
    _, first = repository.claim_next_workflow("first", 60)
    assert repository.claim_next_workflow("second", 60) is None
    with repository.engine.begin() as connection:
        connection.execute(update(workflow_run).values(lease_expires_at=datetime.now(timezone.utc) - timedelta(seconds=1)))
    _, second = repository.claim_next_workflow("second", 60)
    with pytest.raises(LeaseLostError):
        repository.update(record.workflow_id, status=WorkflowStatus.COMPLETED, claim=first)
    assert repository.renew_workflow_claim(first, 60) is False
    assert repository.renew_workflow_claim(second, 60) is True


def test_completion_and_outbox_are_atomic_and_publication_is_recoverable(repository):
    record = add_workflow(repository)
    _, claim = repository.claim_next_workflow("runner", 60)
    event = OutboxEvent(workflow_id=record.workflow_id, event_type="COMPLETION",
                        dedupe_key=f"{record.workflow_id}:completion", topic="completed", key="1", payload={"success": True})
    repository.update(record.workflow_id, status=WorkflowStatus.COMPLETED, claim=claim, outbox=event)
    assert repository.get(record.workflow_id).status == WorkflowStatus.COMPLETED
    first = repository.claim_next_outbox("publisher", 60)
    assert first.event.payload == {"success": True}
    assert repository.claim_next_outbox("other", 60) is None
    repository.reschedule_outbox(first, {"message": "broker unavailable"}, 0)
    second = repository.claim_next_outbox("other", 60)
    assert second.event.event_id == first.event.event_id
    assert second.event.attempt_count == 2
    with pytest.raises(LeaseLostError):
        repository.mark_outbox_published(first)
    repository.mark_outbox_published(second)
    assert repository.get(record.workflow_id).completion_published_at is not None
    assert repository.claim_next_outbox("publisher", 60) is None


def test_lost_claim_cannot_enqueue_completion(repository):
    record = add_workflow(repository)
    _, claim = repository.claim_next_workflow("runner", 60)
    repository.release_workflow_claim(claim)
    event = OutboxEvent(workflow_id=record.workflow_id, event_type="COMPLETION",
                        dedupe_key=f"{record.workflow_id}:completion", topic="completed", key="1", payload={})
    with pytest.raises(LeaseLostError):
        repository.update(record.workflow_id, status=WorkflowStatus.COMPLETED, claim=claim, outbox=event)
    with repository.engine.connect() as connection:
        assert connection.execute(select(workflow_outbox)).first() is None
    assert repository.get(record.workflow_id).status == WorkflowStatus.RECEIVED
