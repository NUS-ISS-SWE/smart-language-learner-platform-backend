from __future__ import annotations

import asyncio
import json
import logging
import socket
from contextlib import suppress
from uuid import uuid4

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from aiokafka.structs import ConsumerRecord
from pydantic import ValidationError

from app.config import Settings
from app.audit import publish as publish_audit
from app.models import OutboxClaim, WorkflowClaim, WorkflowRecord, WorkflowSubmission
from app.observability import record_outbox_publication, record_workflow_execution, workflow_execution_span
from app.repository import LeaseLostError, WorkflowRepository
from app.security import redact_for_trace
from app.workflow import OrchestrationWorkflow

LOGGER = logging.getLogger(__name__)


def worker_identity(component: str) -> str:
    return f"{component}:{socket.gethostname()}:{uuid4()}"


class KafkaWorkflowWorker:
    """Durable Kafka intake only; workflow execution is deliberately decoupled."""

    def __init__(self, settings: Settings, repository: WorkflowRepository, workflow: OrchestrationWorkflow | None = None):
        self.settings = settings
        self.repository = repository
        self.consumer = AIOKafkaConsumer(
            settings.grading_requested_topic,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_group_id,
            enable_auto_commit=False,
            auto_offset_reset="earliest",
            value_deserializer=lambda value: value,
        )
        self.running = False

    async def start(self) -> None:
        await self.consumer.start()
        self.running = True

    async def stop(self) -> None:
        self.running = False
        await self.consumer.stop()

    async def run(self) -> None:
        await self.start()
        try:
            async for message in self.consumer:
                await self.process_message(message)
        finally:
            await self.stop()

    async def process_message(self, message: ConsumerRecord) -> None:
        transport_event_id = self._event_id(message)
        try:
            try:
                submission = self._decode_submission(message.value)
            except (UnicodeDecodeError, json.JSONDecodeError, ValidationError, ValueError) as error:
                LOGGER.error(
                    "discarding_invalid_workflow_message source_event_id=%s error_type=%s",
                    transport_event_id,
                    type(error).__name__,
                )
                await self.consumer.commit()
                return
            source_event_id = submission.idempotency_key(transport_event_id)
            record, created = await asyncio.to_thread(
                self.repository.create_or_get,
                WorkflowRecord(
                    source_event_id=source_event_id,
                    correlation_id=submission.correlation_id,
                    input_payload=submission.model_dump(by_alias=True, mode="json"),
                ),
            )
            LOGGER.info(
                "workflow_intake_persisted workflow_id=%s source_event_id=%s created=%s",
                record.workflow_id,
                source_event_id,
                created,
            )
            await asyncio.to_thread(publish_audit, self.settings, "WORKFLOW_INTAKE_PERSISTED", "SUCCESS", "WORKFLOW", str(record.workflow_id), record.correlation_id)
            await self.consumer.commit()
        except Exception as error:
            LOGGER.error(
                "workflow_intake_failed source_event_id=%s error_type=%s",
                transport_event_id,
                type(error).__name__,
            )
            raise

    @staticmethod
    def _event_id(message: ConsumerRecord) -> str:
        return f"{message.topic}:{message.partition}:{message.offset}"

    @staticmethod
    def _decode_submission(value: object) -> WorkflowSubmission:
        if not isinstance(value, bytes):
            raise ValueError("Kafka grading request must contain bytes")
        payload = json.loads(value.decode("utf-8"))
        if not isinstance(payload, dict):
            raise ValueError("Kafka grading request must be a JSON object")
        return WorkflowSubmission.from_kafka_payload(payload)


class DatabaseWorkflowRunner:
    """Claims and resumes PostgreSQL-backed workflows with fenced leases."""

    def __init__(self, settings: Settings, repository: WorkflowRepository, workflow: OrchestrationWorkflow):
        self.settings = settings
        self.repository = repository
        self.workflow = workflow
        self.owner = worker_identity("workflow")
        self.running = False

    async def run(self) -> None:
        self.running = True
        try:
            while self.running:
                claimed = await asyncio.to_thread(
                    self.repository.claim_next_workflow, self.owner, self.settings.worker_lease_seconds
                )
                if claimed is None:
                    await asyncio.sleep(self.settings.workflow_poll_interval_seconds)
                    continue
                record, claim = claimed
                await self._run_claimed(record, claim)
        finally:
            self.running = False

    async def stop(self) -> None:
        self.running = False

    async def _run_claimed(self, record: WorkflowRecord, claim: WorkflowClaim) -> None:
        heartbeat = asyncio.create_task(self._heartbeat(claim), name=f"workflow-lease-{record.workflow_id}")
        try:
            with workflow_execution_span(record.workflow_id):
                completed = await asyncio.to_thread(self.workflow.run, record, None, claim)
            record_workflow_execution(completed.status.value.lower())
            LOGGER.info("workflow_execution_finished workflow_id=%s", record.workflow_id)
            await asyncio.to_thread(publish_audit, self.settings, "WORKFLOW_EXECUTED", "SUCCESS", "WORKFLOW", str(record.workflow_id), record.correlation_id)
        except LeaseLostError:
            record_workflow_execution("lease_lost")
            LOGGER.warning("workflow_lease_lost workflow_id=%s", record.workflow_id)
        except Exception as error:
            record_workflow_execution("error")
            # Recovery retains the durable input; never invent a student response
            # from an unexpected infrastructure error.
            LOGGER.error(
                "workflow_execution_failed workflow_id=%s error_type=%s",
                record.workflow_id,
                type(error).__name__,
            )
            await asyncio.to_thread(publish_audit, self.settings, "WORKFLOW_EXECUTION_FAILED", "FAILURE", "WORKFLOW", str(record.workflow_id), record.correlation_id)
            with suppress(LeaseLostError):
                await asyncio.to_thread(
                    self.repository.reschedule_workflow,
                    claim,
                    {"message": "unexpected workflow execution failure"},
                    5.0,
                )
        finally:
            heartbeat.cancel()
            with suppress(asyncio.CancelledError):
                await heartbeat
            await asyncio.to_thread(self.repository.release_workflow_claim, claim)

    async def _heartbeat(self, claim: WorkflowClaim) -> None:
        while self.running:
            await asyncio.sleep(self.settings.worker_heartbeat_seconds)
            renewed = await asyncio.to_thread(
                self.repository.renew_workflow_claim, claim, self.settings.worker_lease_seconds
            )
            if not renewed:
                LOGGER.warning("workflow_lease_renewal_failed workflow_id=%s", claim.workflow_id)
                return


class OutboxPublisher:
    """At-least-once Kafka publication for progress and completion events."""

    def __init__(self, settings: Settings, repository: WorkflowRepository):
        self.settings = settings
        self.repository = repository
        self.owner = worker_identity("outbox")
        self.producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda value: json.dumps(value, separators=(",", ":")).encode("utf-8"),
        )
        self.running = False

    async def run(self) -> None:
        await self.producer.start()
        self.running = True
        try:
            while self.running:
                claim = await asyncio.to_thread(
                    self.repository.claim_next_outbox, self.owner, self.settings.outbox_lease_seconds
                )
                if claim is None:
                    await asyncio.sleep(self.settings.outbox_poll_interval_seconds)
                    continue
                await self._publish(claim)
        finally:
            self.running = False
            await self.producer.stop()

    async def stop(self) -> None:
        self.running = False

    async def _publish(self, claim: OutboxClaim) -> None:
        event = claim.event
        try:
            await self.producer.send_and_wait(event.topic, event.payload, key=event.key.encode("utf-8"))
            await asyncio.to_thread(self.repository.mark_outbox_published, claim)
            record_outbox_publication(event.event_type, True)
            LOGGER.info("outbox_event_published event_id=%s type=%s workflow_id=%s", event.event_id, event.event_type, event.workflow_id)
            await asyncio.to_thread(publish_audit, self.settings, "WORKFLOW_OUTBOX_PUBLISHED", "SUCCESS", "WORKFLOW", str(event.workflow_id))
        except LeaseLostError:
            LOGGER.warning("outbox_lease_lost event_id=%s", event.event_id)
        except Exception as error:
            record_outbox_publication(event.event_type, False)
            delay_seconds = min(60.0, float(2 ** min(event.attempt_count, 6)))
            LOGGER.warning(
                "outbox_publish_failed event_id=%s type=%s error_type=%s",
                event.event_id,
                event.event_type,
                type(error).__name__,
            )
            with suppress(LeaseLostError):
                await asyncio.to_thread(
                    self.repository.reschedule_outbox,
                    claim,
                    {"message": redact_for_trace(error), "type": type(error).__name__},
                    delay_seconds,
                )
