from __future__ import annotations

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from sqlalchemy import text

from app.agent_client import AgentClient
from app.agent_registry import AgentRegistry
from app.config import Settings
from app.repository import WorkflowRepository
from app.worker import KafkaWorkflowWorker
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
    app.state.worker = None
    app.state.worker_task = None
    if settings.kafka_enabled:
        worker = KafkaWorkflowWorker(settings, repository, workflow)
        app.state.worker = worker
        app.state.worker_task = asyncio.create_task(worker.run(), name="kafka-workflow-worker")
    try:
        yield
    finally:
        task = app.state.worker_task
        if task:
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass
        agent_client.close()
        if agent_registry:
            agent_registry.close()
        repository.engine.dispose()


app = FastAPI(title="SLLP Orchestrator Service", version="1.0", lifespan=lifespan)


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
    worker = app.state.worker
    if app.state.settings.kafka_enabled and (worker is None or not worker.running):
        raise HTTPException(status_code=503, detail="Kafka worker is unavailable")
    try:
        app.state.agent_client.ensure_ready()
    except Exception as error:
        raise HTTPException(status_code=503, detail="assessment agents are unavailable") from error
    return {"status": "ready"}
