from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from sqlalchemy import text

from app.agent_client import AgentClient
from app.agent_registry import AgentRegistry
from app.config import Settings
from app.observability import configure_observability
from app.repository import WorkflowRepository
from app.worker import DatabaseWorkflowRunner, KafkaWorkflowWorker, OutboxPublisher
from app.workflow import OrchestrationWorkflow

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = Settings.from_environment()
    repository = WorkflowRepository(settings.database_url)
    agent_registry = AgentRegistry(settings) if settings.agent_provider == "a2a" else None
    agent_client = AgentClient(settings, agent_registry)
    workflow = OrchestrationWorkflow(settings, repository, agent_client)
    app.state.settings = settings
    app.state.repository = repository
    app.state.agent_registry = agent_registry
    app.state.agent_client = agent_client
    app.state.intake_worker = None
    app.state.workflow_runner = None
    app.state.outbox_publisher = None
    app.state.worker_tasks = []
    if settings.kafka_enabled:
        intake_worker = KafkaWorkflowWorker(settings, repository)
        workflow_runner = DatabaseWorkflowRunner(settings, repository, workflow)
        outbox_publisher = OutboxPublisher(settings, repository)
        app.state.intake_worker = intake_worker
        app.state.workflow_runner = workflow_runner
        app.state.outbox_publisher = outbox_publisher
        app.state.worker_tasks = [
            asyncio.create_task(intake_worker.run(), name="kafka-workflow-intake"),
            asyncio.create_task(workflow_runner.run(), name="database-workflow-runner"),
            asyncio.create_task(outbox_publisher.run(), name="workflow-outbox-publisher"),
        ]
    try:
        yield
    finally:
        for worker in (app.state.intake_worker, app.state.workflow_runner, app.state.outbox_publisher):
            if worker is not None:
                await worker.stop()
        for task in app.state.worker_tasks:
            task.cancel()
        for task in app.state.worker_tasks:
            try:
                await task
            except asyncio.CancelledError:
                pass
        agent_client.close()
        if agent_registry:
            agent_registry.close()
        repository.engine.dispose()


app = FastAPI(title="SLLP Orchestrator Service", version="1.0", lifespan=lifespan)
configure_observability(app, Settings.from_environment().otel_exporter_otlp_endpoint)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/ready")
def ready() -> dict[str, str]:
    try:
        with app.state.repository.engine.connect() as connection:
            connection.execute(text("SELECT 1"))
    except Exception as error:
        raise HTTPException(status_code=503, detail="database is unavailable") from error
    if app.state.settings.kafka_enabled:
        components = (app.state.intake_worker, app.state.workflow_runner, app.state.outbox_publisher)
        if any(component is None or not component.running for component in components):
            raise HTTPException(status_code=503, detail="orchestrator workers are unavailable")
    try:
        app.state.agent_client.ensure_ready()
    except Exception as error:
        raise HTTPException(status_code=503, detail="assessment agents are unavailable") from error
    return {"status": "ready"}
