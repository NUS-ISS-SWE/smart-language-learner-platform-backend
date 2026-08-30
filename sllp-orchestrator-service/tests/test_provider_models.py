from app.provider_models import A2ASendMessageRequest


def test_a2a_wire_model_serializes_the_expected_json_rpc_shape():
    request = A2ASendMessageRequest.for_assessment("request-1", "workflow-1", {"schemaVersion": "1.0"})

    payload = request.model_dump(by_alias=True, mode="json")

    assert payload["id"] == "request-1"
    assert payload["params"]["message"]["contextId"] == "workflow-1"
    assert payload["params"]["message"]["parts"][0]["mediaType"] == "application/json"
