from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import UUID

from sqlalchemy import BigInteger, Column, DateTime, Integer, MetaData, String, Table, Text, create_engine, insert, select, update
from sqlalchemy.dialects.postgresql import JSONB, UUID as PG_UUID
from sqlalchemy.engine import Engine
from sqlalchemy.exc import IntegrityError

from app.models import WorkflowRecord, WorkflowStatus

metadata = MetaData()

workflow_run = Table(
    "agent_workflow_run",
    metadata,
    Column("workflow_id", PG_UUID(as_uuid=True), primary_key=True),
    Column("source_event_id", String(255), nullable=False, unique=True),
    Column("correlation_id", String(255)),
    Column("composition_id", BigInteger, nullable=False),
    Column("user_id", BigInteger, nullable=False),
    Column("status", String(32), nullable=False),
    Column("input_payload", JSONB, nullable=False),
    Column("grading_output", JSONB),
    Column("review_output", JSONB),
    Column("suggestion_output", JSONB),
    Column("final_report", JSONB),
    Column("completion_event", JSONB),
    Column("completion_published_at", DateTime(timezone=True)),
    Column("error_details", JSONB),
    Column("grading_passes", Integer, nullable=False, default=0),
    Column("created_at", DateTime(timezone=True), nullable=False),
    Column("updated_at", DateTime(timezone=True), nullable=False),
    Column("completed_at", DateTime(timezone=True)),
)

workflow_step_log = Table(
    "agent_workflow_step_log",
    metadata,
    Column("id", BigInteger, primary_key=True),
    Column("workflow_id", PG_UUID(as_uuid=True), nullable=False),
    Column("step", String(64), nullable=False),
    Column("status", String(32), nullable=False),
    Column("attempt", Integer, nullable=False, default=0),
    Column("latency_ms", Integer),
    Column("details", JSONB),
    Column("created_at", DateTime(timezone=True), nullable=False),
)


class WorkflowRepository:
    def __init__(self, database_url: str, engine: Engine | None = None):
        self.engine = engine or create_engine(database_url, pool_pre_ping=True)

    def create_or_get(self, record: WorkflowRecord) -> tuple[WorkflowRecord, bool]:
        values = self._to_values(record)
        try:
            with self.engine.begin() as connection:
                connection.execute(insert(workflow_run).values(**values))
                self.append_log(record.workflow_id, "RECEIVED", record.status, 0, {"sourceEventId": record.source_event_id}, connection)
            return record, True
        except IntegrityError:
            return self.get_by_source_event_id(record.source_event_id), False

    def get_by_source_event_id(self, source_event_id: str) -> WorkflowRecord:
        with self.engine.connect() as connection:
            row = connection.execute(select(workflow_run).where(workflow_run.c.source_event_id == source_event_id)).mappings().one()
        return self._from_row(row)

    def get(self, workflow_id: UUID) -> WorkflowRecord:
        with self.engine.connect() as connection:
            row = connection.execute(select(workflow_run).where(workflow_run.c.workflow_id == workflow_id)).mappings().one()
        return self._from_row(row)

    def update(
        self,
        workflow_id: UUID,
        *,
        status: WorkflowStatus | None = None,
        log_step: str | None = None,
        attempt: int = 0,
        log_details: dict[str, Any] | None = None,
        latency_ms: int | None = None,
        **fields: Any,
    ) -> WorkflowRecord:
        now = datetime.now(timezone.utc)
        values = {key: value for key, value in fields.items() if value is not None}
        if status is not None:
            values["status"] = status.value
            if status in {WorkflowStatus.COMPLETED, WorkflowStatus.FAILED}:
                values["completed_at"] = now
        values["updated_at"] = now
        with self.engine.begin() as connection:
            log_status = status or self._current_status(connection, workflow_id)
            connection.execute(update(workflow_run).where(workflow_run.c.workflow_id == workflow_id).values(**values))
            if log_step:
                self.append_log(workflow_id, log_step, log_status, attempt, log_details or {}, connection, latency_ms)
        return self.get(workflow_id)

    def log_agent_attempts(self, workflow_id: UUID, agent: str, attempt: int, entries: list[dict[str, Any]]) -> None:
        status = self.get(workflow_id).status
        with self.engine.begin() as connection:
            for entry in entries:
                latency_ms = entry.get("latencyMs")
                details = {key: value for key, value in entry.items() if key != "latencyMs"}
                self.append_log(workflow_id, f"{agent.upper()}_ATTEMPT", status, attempt, details, connection, latency_ms)

    def record_failure(self, workflow_id: UUID, error_details: dict[str, Any], completion_event: dict[str, Any]) -> WorkflowRecord:
        return self.update(
            workflow_id,
            status=WorkflowStatus.FAILED,
            log_step="FAILED",
            error_details=error_details,
            completion_event=completion_event,
        )

    def mark_completion_published(self, workflow_id: UUID) -> None:
        self.update(workflow_id, completion_published_at=datetime.now(timezone.utc))

    @staticmethod
    def _current_status(connection: Any, workflow_id: UUID) -> WorkflowStatus:
        value = connection.execute(
            select(workflow_run.c.status).where(workflow_run.c.workflow_id == workflow_id)
        ).scalar_one()
        return WorkflowStatus(value)

    @staticmethod
    def append_log(workflow_id: UUID, step: str, status: WorkflowStatus, attempt: int, details: dict[str, Any], connection: Any, latency_ms: int | None = None) -> None:
        connection.execute(
            insert(workflow_step_log).values(
                workflow_id=workflow_id,
                step=step,
                status=status.value,
                attempt=attempt,
                latency_ms=latency_ms,
                details=details,
                created_at=datetime.now(timezone.utc),
            )
        )

    @staticmethod
    def _to_values(record: WorkflowRecord) -> dict[str, Any]:
        return {
            "workflow_id": record.workflow_id,
            "source_event_id": record.source_event_id,
            "correlation_id": record.correlation_id,
            "composition_id": record.input_payload["composition"].get("compositionId", record.input_payload["composition"].get("composition_id")),
            "user_id": record.input_payload["composition"].get("userId", record.input_payload["composition"].get("user_id")),
            "status": record.status.value,
            "input_payload": record.input_payload,
            "grading_passes": record.grading_passes,
            "created_at": record.created_at,
            "updated_at": record.created_at,
        }

    @staticmethod
    def _from_row(row: Any) -> WorkflowRecord:
        return WorkflowRecord(
            workflow_id=row["workflow_id"],
            source_event_id=row["source_event_id"],
            correlation_id=row["correlation_id"],
            status=WorkflowStatus(row["status"]),
            input_payload=row["input_payload"],
            grading_output=row["grading_output"],
            review_output=row["review_output"],
            suggestion_output=row["suggestion_output"],
            final_report=row["final_report"],
            completion_event=row["completion_event"],
            completion_published_at=row["completion_published_at"],
            error_details=row["error_details"],
            grading_passes=row["grading_passes"],
            created_at=row["created_at"],
        )
