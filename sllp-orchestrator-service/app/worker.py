from __future__ import annotations

import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from aiokafka.structs import ConsumerRecord
from pydantic import ValidationError

from app.config import Settings
from app.models import WorkflowRecord, WorkflowStatus, WorkflowSubmission
from app.repository import WorkflowRepository
from app.workflow import OrchestrationWorkflow

LOGGER = logging.getLogger(__name__)


class KafkaWorkflowWorker:
    def __init__(self, settings: Settings, repository: WorkflowRepository, workflow: OrchestrationWorkflow):
        self.settings = settings
        self.repository = repository
        self.workflow = workflow
        self.consumer = AIOKafkaConsumer(
            settings.grading_requested_topic,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_group_id,
            enable_auto_commit=False,
            auto_offset_reset="earliest",
            value_deserializer=lambda value: value,
        )
        self.producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda value: json.dumps(value, separators=(",", ":")).encode("utf-8"),
        )
        self.running = False
        self._event_loop: asyncio.AbstractEventLoop | None = None

    async def start(self) -> None:
        await self.consumer.start()
        await self.producer.start()
        self.running = True

    async def stop(self) -> None:
        self.running = False
        await self.consumer.stop()
        await self.producer.stop()

    async def run(self) -> None:
        await self.start()
        self._event_loop = asyncio.get_running_loop()
        try:
            async for message in self.consumer:
                await self.process_message(message)
        finally:
            await self.stop()

    async def process_message(self, message: ConsumerRecord) -> None:
        source_event_id = self._event_id(message)
        try:
            try:
                submission = self._decode_submission(message.value)
            except (UnicodeDecodeError, json.JSONDecodeError, ValidationError, ValueError) as error:
                # A message without a valid composition/user pair cannot safely be
                # turned into the platform's completion event. Commit it so one bad
                # producer message never blocks every later assessment.
                LOGGER.error(
                    "discarding_invalid_workflow_message source_event_id=%s error_type=%s",
                    source_event_id,
                    type(error).__name__,
                )
                await self.consumer.commit()
                return
            # Prefer an upstream event ID when a future producer supplies one. Kafka
            # position remains the safe idempotency fallback for today's event schema.
            source_event_id = submission.event_id or source_event_id
            stored_payload = submission.model_dump(by_alias=True, mode="json")
            record, created = await asyncio.to_thread(
                self.repository.create_or_get,
                WorkflowRecord(
                    source_event_id=source_event_id,
                    correlation_id=submission.correlation_id,
                    input_payload=stored_payload,
                ),
            )
            if created or record.status not in {WorkflowStatus.COMPLETED, WorkflowStatus.FAILED}:
                record = await asyncio.to_thread(self.workflow.run, record, self._report_progress)
            if record.completion_event and record.completion_published_at is None:
                await self.producer.send_and_wait(
                    self.settings.grading_completed_topic,
                    record.completion_event,
                    key=str(record.completion_event["compositionId"]).encode("utf-8"),
                )
                await asyncio.to_thread(self.repository.mark_completion_published, record.workflow_id)
            await self.consumer.commit()
        except Exception:
            # Deliberately leave this record uncommitted. A restart/rebalance will
            # replay it and the persisted source event ID prevents duplicate work.
            LOGGER.exception("workflow_message_processing_failed source_event_id=%s", source_event_id)
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

    def _report_progress(self, event: dict[str, object]) -> None:
        """Publish only approved, student-safe status text without blocking grading."""
        if self._event_loop is None:
            return
        try:
            future = asyncio.run_coroutine_threadsafe(
                self.producer.send_and_wait(
                    self.settings.grading_progress_topic,
                    event,
                    key=str(event["compositionId"]).encode("utf-8"),
                ),
                self._event_loop,
            )
            future.result()
        except Exception:
            LOGGER.exception("workflow_progress_publish_failed workflow_id=%s", event.get("workflowId"))
