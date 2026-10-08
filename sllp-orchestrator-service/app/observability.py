from __future__ import annotations

from contextlib import contextmanager
from time import perf_counter
from typing import Iterator

from fastapi import FastAPI
from opentelemetry import trace
from opentelemetry.trace import Status, StatusCode
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from prometheus_client import Counter, Histogram, make_asgi_app


WORKFLOW_EXECUTIONS = Counter(
    "sllp_orchestrator_workflow_executions_total",
    "Durable workflow executions by terminal result.",
    ("result",),
)
WORKFLOW_DURATION = Histogram(
    "sllp_orchestrator_workflow_duration_seconds",
    "End-to-end time spent by one claimed workflow execution.",
)
AGENT_CALLS = Counter(
    "sllp_orchestrator_agent_calls_total",
    "Agent invocations by agent and result.",
    ("agent", "result"),
)
AGENT_CALL_DURATION = Histogram(
    "sllp_orchestrator_agent_call_duration_seconds",
    "Agent invocation duration by agent and outcome.",
    ("agent", "result"),
)
AGENT_CONTRACT_REJECTIONS = Counter(
    "sllp_orchestrator_agent_contract_rejections_total",
    "Rejected peer artifacts by agent and bounded reason.",
    ("agent", "reason"),
)
WORKFLOW_BUDGET_GUARDS = Counter(
    "sllp_orchestrator_workflow_budget_guards_total",
    "Workflows stopped before another agent call because their configured spend budget was exhausted.",
)
OUTBOX_PUBLICATIONS = Counter(
    "sllp_orchestrator_outbox_publications_total",
    "Outbox publication attempts by event type and result.",
    ("event_type", "result"),
)

TRACER = trace.get_tracer("sllp.orchestrator")


def configure_observability(app: FastAPI, otlp_endpoint: str | None) -> None:
    resource = Resource.create({"service.name": "sllp-orchestrator-service"})
    provider = TracerProvider(resource=resource)
    if otlp_endpoint:
        from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

        provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=otlp_endpoint)))
    trace.set_tracer_provider(provider)
    FastAPIInstrumentor.instrument_app(app)
    HTTPXClientInstrumentor().instrument()
    app.mount("/metrics", make_asgi_app())


@contextmanager
def workflow_execution_span(workflow_id: object) -> Iterator[None]:
    started = perf_counter()
    with TRACER.start_as_current_span("workflow.execute", record_exception=False, set_status_on_exception=False) as span:
        span.set_attribute("sllp.workflow_id", str(workflow_id))
        try:
            yield
        except Exception as error:
            # Database exceptions may include SQL parameters containing student text.
            span.set_attribute("error.type", type(error).__name__)
            span.set_status(Status(StatusCode.ERROR))
            raise
        finally:
            WORKFLOW_DURATION.observe(perf_counter() - started)


def record_workflow_execution(result: str) -> None:
    WORKFLOW_EXECUTIONS.labels(result=result).inc()


def record_agent_call(agent: str, succeeded: bool, latency_ms: int = 0) -> None:
    result = "success" if succeeded else "failure"
    AGENT_CALLS.labels(agent=agent, result=result).inc()
    AGENT_CALL_DURATION.labels(agent=agent, result=result).observe(max(latency_ms, 0) / 1_000)


def record_agent_contract_rejection(agent: str, reason: str) -> None:
    AGENT_CONTRACT_REJECTIONS.labels(agent=agent, reason=reason).inc()


def record_workflow_budget_guard() -> None:
    WORKFLOW_BUDGET_GUARDS.inc()


def record_outbox_publication(event_type: str, succeeded: bool) -> None:
    OUTBOX_PUBLICATIONS.labels(event_type=event_type, result="success" if succeeded else "failure").inc()
