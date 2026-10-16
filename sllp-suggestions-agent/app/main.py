from __future__ import annotations

from contextlib import asynccontextmanager
import json
from time import perf_counter
from typing import Any
from uuid import uuid4

import httpx
from fastapi import FastAPI, HTTPException, Request
from prometheus_client import make_asgi_app
from starlette.concurrency import run_in_threadpool

from app.config import Settings
from app.framework import AdminRubricFrameworkProvider
from app.generator import OpenAIChatGenerator
from app.metrics import A2A_DURATION, A2A_REQUESTS, record_result
from app.models import SuggestionRequest, SuggestionResponse
from app.security import bearer_token_matches
from app.service import SuggestionService, SuggestionServiceError
from app.tools import LearnerProfileTool


def _a2a_error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


def _completed_task(request_id: Any, context_id: str, artifact: dict[str, Any]) -> dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "task": {
                "id": str(uuid4()),
                "contextId": context_id,
                "status": {"state": "TASK_STATE_COMPLETED"},
                "artifacts": [{"artifactId": str(uuid4()), "parts": [{"mediaType": "application/json", "data": artifact}]}],
            }
        },
    }


def _extract_request(body: dict[str, Any]) -> tuple[Any, str, dict[str, Any]]:
    if not isinstance(body, dict):
        raise ValueError("A2A request must be an object")
    if body.get("jsonrpc") != "2.0" or body.get("method") != "SendMessage":
        raise ValueError("only A2A SendMessage requests are supported")
    request_id = body.get("id")
    message = body["params"]["message"]
    context_id = message["contextId"]
    for part in message["parts"]:
        if part.get("mediaType") == "application/json" and isinstance(part.get("data"), dict):
            return request_id, context_id, part["data"]
    raise ValueError("A2A request must contain one application/json data part")


def _draft_2020_schema(model: type[Any]) -> dict[str, Any]:
    """Publish Pydantic's generated contract with an explicit JSON Schema dialect."""
    return {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        **model.model_json_schema(by_alias=True),
    }


def _suggestion_output_schema() -> dict[str, Any]:
    """Add response-state constraints that Pydantic's JSON Schema cannot express."""
    schema = _draft_2020_schema(SuggestionResponse)
    schema["required"] = list(dict.fromkeys([*schema.get("required", []), "suggestionStatus", "suggestions"]))
    schema["allOf"] = [
        {
            "if": {"properties": {"suggestionStatus": {"const": "AVAILABLE"}}, "required": ["suggestionStatus"]},
            "then": {"properties": {"suggestions": {"minItems": 1}}},
        },
        {
            "if": {
                "properties": {
                    "suggestionStatus": {
                        "enum": ["NOT_AVAILABLE_FOR_LANGUAGE", "TEMPORARILY_UNAVAILABLE", "NO_PRIORITY_IMPROVEMENT"]
                    }
                },
                "required": ["suggestionStatus"],
            },
            "then": {"properties": {"suggestions": {"maxItems": 0}}},
        },
    ]
    return schema


async def _read_bounded_body(request: Request, limit: int) -> bytes:
    """Reject an oversized request while streaming, before buffering it in memory."""
    declared = request.headers.get("content-length")
    if declared and declared.isdigit() and int(declared) > limit:
        raise HTTPException(status_code=413, detail="A2A request is too large")
    chunks: list[bytes] = []
    total = 0
    async for chunk in request.stream():
        total += len(chunk)
        if total > limit:
            raise HTTPException(status_code=413, detail="A2A request is too large")
        chunks.append(chunk)
    return b"".join(chunks)


def create_app(service: SuggestionService | None = None, settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings.from_environment()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        client = httpx.Client(timeout=settings.http_timeout_seconds)
        app.state.client = client
        app.state.service = service or SuggestionService(
            LearnerProfileTool(settings.user_service_url, client),
            AdminRubricFrameworkProvider(settings.admin_service_url, client),
            OpenAIChatGenerator(settings.openai_api_key, settings.openai_base_url, settings.openai_model, settings.openai_temperature, client,
                provider=settings.usage_provider, pricing_version=settings.pricing_version,
                input_price_micros_per_million=settings.input_price_micros_per_million,
                cached_input_price_micros_per_million=settings.cached_input_price_micros_per_million,
                output_price_micros_per_million=settings.output_price_micros_per_million,
                max_completion_tokens=settings.max_completion_tokens),
        )
        try:
            yield
        finally:
            client.close()

    app = FastAPI(title="SLLP Suggestions Agent", version="1.0", lifespan=lifespan)
    app.mount("/metrics", make_asgi_app())

    @app.get("/.well-known/agent-card.json")
    def agent_card() -> dict[str, Any]:
        a2a_url = f"{settings.public_base_url}/a2a"
        bearer_requirement = [{"schemes": {"peerBearer": {"list": []}}}]
        return {
            "name": "SLLP Suggestions Agent",
            "description": "Creates short, evidence-grounded English writing improvements after an assessment has been approved.",
            # Retained for the SLLP v2 profile; supportedInterfaces is the A2A v1 declaration.
            "url": a2a_url,
            "protocolVersion": "1.0",
            "version": settings.agent_version,
            "supportedInterfaces": [{"url": a2a_url, "protocolBinding": "JSONRPC", "protocolVersion": "1.0"}],
            "capabilities": {"streaming": False, "pushNotifications": False, "extendedAgentCard": False},
            "defaultInputModes": ["application/json"],
            "defaultOutputModes": ["application/json"],
            "securitySchemes": {
                "peerBearer": {
                    "httpAuthSecurityScheme": {
                        "scheme": "Bearer",
                        "description": "SLLP peer bearer token in the Authorization header.",
                    }
                }
            },
            "securityRequirements": bearer_requirement,
            "skills": [{
                "id": "writing-suggestions",
                "name": "Writing suggestions",
                "description": "Returns up to three actionable English composition improvements grounded in approved grading evidence.",
                "tags": ["assessment", "writing", "coaching", "evidence"],
                "inputModes": ["application/json"],
                "outputModes": ["application/json"],
                "securityRequirements": bearer_requirement,
            }],
            "sllp": {
                "contractVersion": "2.0",
                "inputSchemaUrl": f"{settings.public_base_url}/.well-known/sllp-contracts/writing-suggestions-v2-input.json",
                "outputSchemaUrl": f"{settings.public_base_url}/.well-known/sllp-contracts/writing-suggestions-v2-output.json",
            },
        }

    @app.get("/.well-known/sllp-contracts/writing-suggestions-v2-input.json")
    def suggestion_input_schema() -> dict[str, Any]:
        return _draft_2020_schema(SuggestionRequest)

    @app.get("/.well-known/sllp-contracts/writing-suggestions-v2-output.json")
    def suggestion_output_schema() -> dict[str, Any]:
        return _suggestion_output_schema()

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.get("/ready")
    def ready() -> dict[str, str]:
        try:
            app.state.service.ready()
        except Exception as error:
            raise HTTPException(status_code=503, detail="Admin rubric is unavailable") from error
        return {"status": "ready"}

    @app.post("/a2a")
    async def a2a(request: Request) -> dict[str, Any]:
        authorization = request.headers.get("Authorization", "")
        if not bearer_token_matches(authorization, settings.a2a_token):
            A2A_REQUESTS.labels(outcome="unauthorized").inc()
            raise HTTPException(status_code=401, detail="unauthorized")
        started = perf_counter()
        request_id = None
        try:
            try:
                raw_body = await _read_bounded_body(request, settings.max_a2a_request_bytes)
            except HTTPException as error:
                if error.status_code == 413:
                    A2A_REQUESTS.labels(outcome="too_large").inc()
                raise
            body = json.loads(raw_body)
            request_id = body.get("id") if isinstance(body, dict) else None
            request_id, context_id, data = _extract_request(body)
            parsed = SuggestionRequest.model_validate(data)
            result = await run_in_threadpool(app.state.service.generate, parsed)
            record_result(result)
            A2A_REQUESTS.labels(outcome="success").inc()
            artifact = result.model_dump(by_alias=True, mode="json")
            return _completed_task(request_id, context_id, artifact)
        except (ValueError, KeyError, TypeError, AttributeError):
            A2A_REQUESTS.labels(outcome="invalid_request").inc()
            return _a2a_error(request_id, -32602, "invalid A2A suggestion request")
        except SuggestionServiceError as error:
            A2A_REQUESTS.labels(outcome="service_error").inc()
            return _a2a_error(request_id, -32001, str(error))
        finally:
            A2A_DURATION.observe(perf_counter() - started)

    return app


app = create_app()
