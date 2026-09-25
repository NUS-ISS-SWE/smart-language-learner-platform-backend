from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field


AgentName = Literal["evidence", "grading", "review", "suggestion"]


class _WireModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")


class A2ADataPart(_WireModel):
    data: dict[str, Any]
    media_type: str = Field("application/json", serialization_alias="mediaType")


class A2AMessage(_WireModel):
    message_id: str = Field(serialization_alias="messageId")
    context_id: str = Field(serialization_alias="contextId")
    role: Literal["ROLE_USER"] = "ROLE_USER"
    parts: list[A2ADataPart]


class A2AConfiguration(_WireModel):
    accepted_output_modes: list[Literal["application/json"]] = Field(serialization_alias="acceptedOutputModes")


class A2AParameters(_WireModel):
    message: A2AMessage
    configuration: A2AConfiguration


class A2ASendMessageRequest(_WireModel):
    jsonrpc: Literal["2.0"] = "2.0"
    request_id: str = Field(serialization_alias="id")
    method: Literal["SendMessage"] = "SendMessage"
    params: A2AParameters

    @classmethod
    def for_assessment(cls, request_id: str, workflow_id: str, payload: dict[str, Any]) -> "A2ASendMessageRequest":
        return cls(
            request_id=request_id,
            params=A2AParameters(
                message=A2AMessage(message_id=request_id, context_id=workflow_id, parts=[A2ADataPart(data=payload)]),
                configuration=A2AConfiguration(accepted_output_modes=["application/json"]),
            ),
        )
