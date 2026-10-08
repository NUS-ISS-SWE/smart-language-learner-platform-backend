from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any
from uuid import NAMESPACE_URL, UUID, uuid5

from sqlalchemy import BigInteger, Column, DateTime, Integer, MetaData, String, Table, create_engine, func, insert, or_, select, update
from sqlalchemy.dialects.postgresql import JSONB, UUID as PG_UUID, insert as pg_insert
from sqlalchemy.engine import Engine
from sqlalchemy.exc import IntegrityError

from app.models import (
    AgentUsage,
    OutboxClaim,
    OutboxEvent,
    TERMINAL_WORKFLOW_STATUSES,
    WorkflowClaim,
    WorkflowRecord,
    WorkflowStatus,
)

metadata = MetaData()


class LeaseLostError(RuntimeError):
    """Raised when another worker has recovered a workflow or outbox message."""

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
    Column("evidence_output", JSONB),
    Column("grading_output", JSONB),
    Column("review_output", JSONB),
    Column("suggestion_output", JSONB),
    Column("final_report", JSONB),
    Column("completion_event", JSONB),
    Column("completion_published_at", DateTime(timezone=True)),
    Column("error_details", JSONB),
    Column("grading_passes", Integer, nullable=False, default=0),
    Column("claimed_by", String(255)),
    Column("lease_expires_at", DateTime(timezone=True)),
    Column("next_run_at", DateTime(timezone=True), nullable=False),
    Column("lock_version", Integer, nullable=False, default=0),
    Column("created_at", DateTime(timezone=True), nullable=False),
    Column("updated_at", DateTime(timezone=True), nullable=False),
    Column("completed_at", DateTime(timezone=True)),
)

workflow_outbox = Table(
    "agent_workflow_outbox",
    metadata,
    Column("event_id", PG_UUID(as_uuid=True), primary_key=True),
    Column("workflow_id", PG_UUID(as_uuid=True), nullable=False),
    Column("event_type", String(32), nullable=False),
    Column("dedupe_key", String(255), nullable=False, unique=True),
    Column("topic", String(255), nullable=False),
    Column("message_key", String(255), nullable=False),
    Column("payload", JSONB, nullable=False),
    Column("attempt_count", Integer, nullable=False, default=0),
    Column("claimed_by", String(255)),
    Column("lease_expires_at", DateTime(timezone=True)),
    Column("next_attempt_at", DateTime(timezone=True), nullable=False),
    Column("lock_version", Integer, nullable=False, default=0),
    Column("last_error", JSONB),
    Column("published_at", DateTime(timezone=True)),
    Column("created_at", DateTime(timezone=True), nullable=False),
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

ai_usage_event = Table(
    "ai_usage_event",
    metadata,
    Column("event_id", PG_UUID(as_uuid=True), primary_key=True),
    Column("request_id", PG_UUID(as_uuid=True)),
    Column("workflow_id", PG_UUID(as_uuid=True)),
    Column("user_id", BigInteger, nullable=False),
    Column("composition_id", BigInteger),
    Column("operation", String(50), nullable=False),
    Column("agent", String(50), nullable=False),
    Column("provider", String(50), nullable=False),
    Column("model", String(100), nullable=False),
    Column("prompt_tokens", BigInteger, nullable=False),
    Column("completion_tokens", BigInteger, nullable=False),
    Column("cached_input_tokens", BigInteger, nullable=False),
    Column("total_tokens", BigInteger, nullable=False),
    Column("estimated_cost_micros", BigInteger, nullable=False),
    Column("currency", String(3), nullable=False),
    Column("pricing_version", String(100), nullable=False),
    Column("input_price_micros_per_million", BigInteger, nullable=False),
    Column("cached_input_price_micros_per_million", BigInteger, nullable=False),
    Column("output_price_micros_per_million", BigInteger, nullable=False),
    Column("created_at", DateTime(timezone=False), nullable=False),
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
        claim: WorkflowClaim | None = None,
        outbox: OutboxEvent | None = None,
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
            statement = update(workflow_run).where(workflow_run.c.workflow_id == workflow_id)
            if claim is not None:
                statement = statement.where(
                    workflow_run.c.claimed_by == claim.owner,
                    workflow_run.c.lock_version == claim.lock_version,
                    workflow_run.c.lease_expires_at > now,
                )
            result = connection.execute(statement.values(**values))
            if result.rowcount != 1:
                raise LeaseLostError(f"workflow lease is no longer held: {workflow_id}")
            if log_step:
                self.append_log(workflow_id, log_step, log_status, attempt, log_details or {}, connection, latency_ms)
            if outbox is not None:
                self._enqueue_outbox(connection, outbox, now)
        return self.get(workflow_id)

    def log_agent_attempts(
        self,
        workflow_id: UUID,
        agent: str,
        attempt: int,
        entries: list[dict[str, Any]],
        claim: WorkflowClaim | None = None,
    ) -> None:
        status = self.get(workflow_id).status
        with self.engine.begin() as connection:
            self._assert_claim(connection, workflow_id, claim)
            for entry in entries:
                latency_ms = entry.get("latencyMs")
                details = {key: value for key, value in entry.items() if key != "latencyMs"}
                self.append_log(workflow_id, f"{agent.upper()}_ATTEMPT", status, attempt, details, connection, latency_ms)

    def record_failure(
        self,
        workflow_id: UUID,
        error_details: dict[str, Any],
        completion_event: dict[str, Any],
        claim: WorkflowClaim | None = None,
        outbox: OutboxEvent | None = None,
    ) -> WorkflowRecord:
        return self.update(
            workflow_id,
            status=WorkflowStatus.FAILED,
            log_step="FAILED",
            error_details=error_details,
            completion_event=completion_event,
            claim=claim,
            outbox=outbox,
        )

    def claim_next_workflow(self, owner: str, lease_seconds: int) -> tuple[WorkflowRecord, WorkflowClaim] | None:
        """Atomically acquire one recoverable workflow, skipping work held by peers."""
        now = datetime.now(timezone.utc)
        with self.engine.begin() as connection:
            row = connection.execute(
                select(workflow_run)
                .where(
                    workflow_run.c.status.not_in([status.value for status in TERMINAL_WORKFLOW_STATUSES]),
                    workflow_run.c.next_run_at <= now,
                    or_(workflow_run.c.claimed_by.is_(None), workflow_run.c.lease_expires_at <= now),
                )
                .order_by(workflow_run.c.next_run_at, workflow_run.c.created_at)
                .limit(1)
                .with_for_update(skip_locked=True)
            ).mappings().first()
            if row is None:
                return None
            lock_version = row["lock_version"] + 1
            result = connection.execute(
                update(workflow_run)
                .where(workflow_run.c.workflow_id == row["workflow_id"], workflow_run.c.lock_version == row["lock_version"])
                .values(
                    claimed_by=owner,
                    lease_expires_at=now + timedelta(seconds=lease_seconds),
                    lock_version=lock_version,
                    updated_at=now,
                )
            )
            if result.rowcount != 1:
                return None
            claimed = dict(row)
            claimed.update(claimed_by=owner, lease_expires_at=now + timedelta(seconds=lease_seconds), lock_version=lock_version)
            record = self._from_row(claimed)
            return record, WorkflowClaim(workflow_id=record.workflow_id, owner=owner, lock_version=lock_version)

    def renew_workflow_claim(self, claim: WorkflowClaim, lease_seconds: int) -> bool:
        now = datetime.now(timezone.utc)
        with self.engine.begin() as connection:
            result = connection.execute(
                update(workflow_run)
                .where(
                    workflow_run.c.workflow_id == claim.workflow_id,
                    workflow_run.c.claimed_by == claim.owner,
                    workflow_run.c.lock_version == claim.lock_version,
                    workflow_run.c.lease_expires_at > now,
                )
                .values(lease_expires_at=now + timedelta(seconds=lease_seconds), updated_at=now)
            )
        return result.rowcount == 1

    def release_workflow_claim(self, claim: WorkflowClaim) -> None:
        with self.engine.begin() as connection:
            connection.execute(
                update(workflow_run)
                .where(
                    workflow_run.c.workflow_id == claim.workflow_id,
                    workflow_run.c.claimed_by == claim.owner,
                    workflow_run.c.lock_version == claim.lock_version,
                )
                .values(claimed_by=None, lease_expires_at=None)
            )

    def reschedule_workflow(self, claim: WorkflowClaim, error_details: dict[str, Any], delay_seconds: float) -> None:
        now = datetime.now(timezone.utc)
        with self.engine.begin() as connection:
            status = self._current_status(connection, claim.workflow_id)
            result = connection.execute(
                update(workflow_run)
                .where(
                    workflow_run.c.workflow_id == claim.workflow_id,
                    workflow_run.c.claimed_by == claim.owner,
                    workflow_run.c.lock_version == claim.lock_version,
                )
                .values(
                    claimed_by=None,
                    lease_expires_at=None,
                    next_run_at=now + timedelta(seconds=delay_seconds),
                    error_details=error_details,
                    updated_at=now,
                )
            )
            if result.rowcount != 1:
                raise LeaseLostError(f"workflow lease is no longer held: {claim.workflow_id}")
            self.append_log(claim.workflow_id, "RECOVERY_SCHEDULED", status, 0, error_details, connection)

    def record_agent_usage(
        self,
        *,
        workflow_id: UUID,
        request_id: UUID,
        user_id: int,
        composition_id: int,
        agent: str,
        attempt: int,
        usage: AgentUsage,
        claim: WorkflowClaim | None = None,
        invocation_id: str | None = None,
    ) -> None:
        """Persist one idempotent, provider-reported usage event per successful agent call."""
        values = self.agent_usage_values(
            workflow_id=workflow_id,
            request_id=request_id,
            user_id=user_id,
            composition_id=composition_id,
            agent=agent,
            attempt=attempt,
            usage=usage,
            invocation_id=invocation_id,
        )
        with self.engine.begin() as connection:
            self._assert_claim(connection, workflow_id, claim)
            connection.execute(pg_insert(ai_usage_event).values(**values).on_conflict_do_nothing(
                index_elements=[ai_usage_event.c.event_id]
            ))

    def workflow_cost_micros(self, workflow_id: UUID) -> int:
        """Return only the aggregate cost needed for a pre-invocation budget guard."""
        with self.engine.connect() as connection:
            value = connection.execute(
                select(func.coalesce(func.sum(ai_usage_event.c.estimated_cost_micros), 0)).where(
                    ai_usage_event.c.workflow_id == workflow_id
                )
            ).scalar_one()
        return int(value)

    def claim_next_outbox(self, owner: str, lease_seconds: int) -> OutboxClaim | None:
        now = datetime.now(timezone.utc)
        with self.engine.begin() as connection:
            row = connection.execute(
                select(workflow_outbox)
                .where(
                    workflow_outbox.c.published_at.is_(None),
                    workflow_outbox.c.next_attempt_at <= now,
                    or_(workflow_outbox.c.claimed_by.is_(None), workflow_outbox.c.lease_expires_at <= now),
                )
                .order_by(workflow_outbox.c.next_attempt_at, workflow_outbox.c.created_at)
                .limit(1)
                .with_for_update(skip_locked=True)
            ).mappings().first()
            if row is None:
                return None
            lock_version = row["lock_version"] + 1
            result = connection.execute(
                update(workflow_outbox)
                .where(workflow_outbox.c.event_id == row["event_id"], workflow_outbox.c.lock_version == row["lock_version"])
                .values(
                    claimed_by=owner,
                    lease_expires_at=now + timedelta(seconds=lease_seconds),
                    lock_version=lock_version,
                    attempt_count=row["attempt_count"] + 1,
                )
            )
            if result.rowcount != 1:
                return None
            event = OutboxEvent(
                event_id=row["event_id"],
                workflow_id=row["workflow_id"],
                event_type=row["event_type"],
                dedupe_key=row["dedupe_key"],
                topic=row["topic"],
                key=row["message_key"],
                payload=row["payload"],
                attempt_count=row["attempt_count"] + 1,
            )
            return OutboxClaim(event=event, owner=owner, lock_version=lock_version)

    def mark_outbox_published(self, claim: OutboxClaim) -> None:
        now = datetime.now(timezone.utc)
        with self.engine.begin() as connection:
            result = connection.execute(
                update(workflow_outbox)
                .where(
                    workflow_outbox.c.event_id == claim.event.event_id,
                    workflow_outbox.c.claimed_by == claim.owner,
                    workflow_outbox.c.lock_version == claim.lock_version,
                )
                .values(published_at=now, claimed_by=None, lease_expires_at=None, last_error=None)
            )
            if result.rowcount != 1:
                raise LeaseLostError(f"outbox lease is no longer held: {claim.event.event_id}")
            if claim.event.event_type == "COMPLETION":
                connection.execute(
                    update(workflow_run)
                    .where(workflow_run.c.workflow_id == claim.event.workflow_id)
                    .values(completion_published_at=now, updated_at=now)
                )

    def reschedule_outbox(self, claim: OutboxClaim, error_details: dict[str, Any], delay_seconds: float) -> None:
        now = datetime.now(timezone.utc)
        with self.engine.begin() as connection:
            result = connection.execute(
                update(workflow_outbox)
                .where(
                    workflow_outbox.c.event_id == claim.event.event_id,
                    workflow_outbox.c.claimed_by == claim.owner,
                    workflow_outbox.c.lock_version == claim.lock_version,
                )
                .values(
                    claimed_by=None,
                    lease_expires_at=None,
                    next_attempt_at=now + timedelta(seconds=delay_seconds),
                    last_error=error_details,
                )
            )
            if result.rowcount != 1:
                raise LeaseLostError(f"outbox lease is no longer held: {claim.event.event_id}")

    @staticmethod
    def agent_usage_values(
        *,
        workflow_id: UUID,
        request_id: UUID,
        user_id: int,
        composition_id: int,
        agent: str,
        attempt: int,
        usage: AgentUsage,
        invocation_id: str | None = None,
    ) -> dict[str, Any]:
        """Build the immutable ledger row separately so cost arithmetic is directly testable."""
        identity = f"sllp-agent-usage:{workflow_id}:{agent}:{attempt}"
        if invocation_id is not None:
            identity += f":{invocation_id}"
        event_id = uuid5(NAMESPACE_URL, identity)
        total_tokens = usage.total_tokens or usage.prompt_tokens + usage.completion_tokens
        non_cached_prompt_tokens = usage.prompt_tokens - usage.cached_input_tokens
        estimated_cost_micros = WorkflowRepository._round_micros(
            non_cached_prompt_tokens * usage.input_price_micros_per_million
            + usage.cached_input_tokens * usage.cached_input_price_micros_per_million
            + usage.completion_tokens * usage.output_price_micros_per_million
        )
        return {
            "event_id": event_id,
            "request_id": request_id,
            "workflow_id": workflow_id,
            "user_id": user_id,
            "composition_id": composition_id,
            "operation": "COMPOSITION_ASSESSMENT",
            "agent": agent,
            "provider": usage.provider,
            "model": usage.model,
            "prompt_tokens": usage.prompt_tokens,
            "completion_tokens": usage.completion_tokens,
            "cached_input_tokens": usage.cached_input_tokens,
            "total_tokens": total_tokens,
            "estimated_cost_micros": estimated_cost_micros,
            "currency": "USD",
            "pricing_version": usage.pricing_version,
            "input_price_micros_per_million": usage.input_price_micros_per_million,
            "cached_input_price_micros_per_million": usage.cached_input_price_micros_per_million,
            "output_price_micros_per_million": usage.output_price_micros_per_million,
            "created_at": datetime.now(timezone.utc).replace(tzinfo=None),
        }
    @staticmethod
    def _cost(tokens: int, micros_per_million: int) -> int:
        # Half-up rounding in integer arithmetic; values are non-negative by model validation.
        return WorkflowRepository._round_micros(tokens * micros_per_million)

    @staticmethod
    def _round_micros(numerator: int) -> int:
        return (numerator + 500_000) // 1_000_000

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
    def _assert_claim(connection: Any, workflow_id: UUID, claim: WorkflowClaim | None) -> None:
        if claim is None:
            return
        now = datetime.now(timezone.utc)
        held = connection.execute(
            select(workflow_run.c.workflow_id).where(
                workflow_run.c.workflow_id == workflow_id,
                workflow_run.c.claimed_by == claim.owner,
                workflow_run.c.lock_version == claim.lock_version,
                workflow_run.c.lease_expires_at > now,
            )
        ).scalar_one_or_none()
        if held is None:
            raise LeaseLostError(f"workflow lease is no longer held: {workflow_id}")

    @staticmethod
    def _enqueue_outbox(connection: Any, event: OutboxEvent, now: datetime) -> None:
        connection.execute(
            pg_insert(workflow_outbox)
            .values(
                event_id=event.event_id,
                workflow_id=event.workflow_id,
                event_type=event.event_type,
                dedupe_key=event.dedupe_key,
                topic=event.topic,
                message_key=event.key,
                payload=event.payload,
                next_attempt_at=now,
                created_at=now,
            )
            .on_conflict_do_nothing(index_elements=[workflow_outbox.c.dedupe_key])
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
            "evidence_output": record.evidence_output,
            "grading_passes": record.grading_passes,
            "next_run_at": record.created_at,
            "lock_version": 0,
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
            evidence_output=row["evidence_output"],
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
