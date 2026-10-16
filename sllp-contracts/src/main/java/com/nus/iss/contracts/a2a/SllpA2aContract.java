package com.nus.iss.contracts.a2a;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared SLLP A2A Agent Card metadata and JSON Schema fragments. */
public final class SllpA2aContract {
    public static final String PROTOCOL_VERSION = "1.0";
    public static final String CONTRACT_VERSION = "2.0";
    public static final String JSON_MEDIA_TYPE = "application/json";
    public static final String JSON_SCHEMA_DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private SllpA2aContract() {
    }

    public static Map<String, Object> agentCard(
            String name,
            String description,
            String agentVersion,
            String baseUrl,
            String skillId,
            String skillName,
            String skillDescription,
            List<String> tags,
            String inputSchemaPath,
            String outputSchemaPath) {
        String a2aUrl = baseUrl + "/a2a";
        List<Map<String, Object>> bearerRequirement = bearerSecurityRequirement();
        return Map.ofEntries(
                Map.entry("name", name),
                Map.entry("description", description),
                // Retained for the SLLP v2 profile; supportedInterfaces is the A2A v1 declaration.
                Map.entry("url", a2aUrl),
                Map.entry("protocolVersion", PROTOCOL_VERSION),
                Map.entry("version", agentVersion),
                Map.entry("supportedInterfaces", List.of(Map.of(
                        "url", a2aUrl,
                        "protocolBinding", "JSONRPC",
                        "protocolVersion", PROTOCOL_VERSION))),
                Map.entry("capabilities", Map.of(
                        "streaming", false,
                        "pushNotifications", false,
                        "extendedAgentCard", false)),
                Map.entry("defaultInputModes", List.of(JSON_MEDIA_TYPE)),
                Map.entry("defaultOutputModes", List.of(JSON_MEDIA_TYPE)),
                Map.entry("securitySchemes", bearerSecurityScheme()),
                Map.entry("securityRequirements", bearerRequirement),
                Map.entry("skills", List.of(Map.of(
                        "id", skillId,
                        "name", skillName,
                        "description", skillDescription,
                        "tags", tags,
                        "inputModes", List.of(JSON_MEDIA_TYPE),
                        "outputModes", List.of(JSON_MEDIA_TYPE),
                        "securityRequirements", bearerRequirement))),
                Map.entry("sllp", Map.of(
                        "contractVersion", CONTRACT_VERSION,
                        "inputSchemaUrl", baseUrl + inputSchemaPath,
                        "outputSchemaUrl", baseUrl + outputSchemaPath)));
    }

    public static Map<String, Object> objectSchema(
            String title,
            List<String> required,
            Map<String, Object> properties) {
        return Map.of(
                "$schema", JSON_SCHEMA_DRAFT_2020_12,
                "title", title,
                "type", "object",
                "required", required,
                "properties", properties);
    }

    /** A fresh map so individual agent schemas can add their required artifact fields. */
    public static Map<String, Object> commonRequestProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("schemaVersion", Map.of("const", CONTRACT_VERSION));
        properties.put("workflowId", Map.of("type", "string", "format", "uuid"));
        properties.put("correlationId", Map.of("type", "string", "minLength", 1));
        properties.put("attempt", Map.of("type", "integer", "minimum", 1));
        properties.put("composition", compositionSchema());
        properties.put("priorGrading", Map.of());
        properties.put("instructions", instructionSchema());
        return properties;
    }

    public static Map<String, Object> compositionSchema() {
        return Map.of("type", "object", "required", List.of("compositionId", "userId", "title", "content", "language"), "properties", Map.of(
                "compositionId", Map.of("type", "integer", "minimum", 1),
                "userId", Map.of("type", "integer", "minimum", 1),
                "title", Map.of("type", "string", "minLength", 1, "maxLength", 500),
                "content", Map.of("type", "string", "minLength", 1, "maxLength", 20000),
                "language", Map.of("type", "string", "enum", List.of("ENGLISH", "CHINESE")),
                "requestedAt", Map.of("type", "string", "format", "date-time")));
    }

    public static Map<String, Object> evidenceArtifactSchema() {
        return Map.of("type", "object", "required", List.of("criteria", "rubricVersion", "rubricSnapshot"), "properties", Map.of(
                "criteria", evidenceCriteriaSchema(),
                "rubricVersion", Map.of("type", "string", "minLength", 1),
                "rubricSnapshot", rubricSnapshotSchema()));
    }

    public static Map<String, Object> evidenceCriteriaSchema() {
        return Map.of("type", "array", "minItems", 1, "items", Map.of("type", "object",
                "required", List.of("criterion", "evidence"), "properties", Map.of(
                        "criterion", Map.of("type", "string", "minLength", 1),
                        "evidence", Map.of("type", "array", "minItems", 1, "items", Map.of("type", "object",
                                "required", List.of("excerpt", "location", "polarity", "note", "verifiedOnFirstPass"), "properties", Map.of(
                                        "excerpt", Map.of("type", "string", "minLength", 1, "maxLength", 2000),
                                        "location", Map.of("type", "string", "minLength", 1),
                                        "polarity", Map.of("type", "string", "enum", List.of("STRENGTH", "WEAKNESS")),
                                        "note", Map.of("type", "string", "minLength", 1),
                                        "verifiedOnFirstPass", Map.of("type", "boolean")))))));
    }

    public static Map<String, Object> rubricSnapshotSchema() {
        return Map.of("type", "object", "required", List.of("version", "criteria"), "properties", Map.of(
                "version", Map.of("type", "string", "minLength", 1),
                "criteria", Map.of("type", "array", "minItems", 1, "items", Map.of("type", "object",
                        "required", List.of("name", "weight", "guidance"), "properties", Map.of(
                                "name", Map.of("type", "string", "minLength", 1),
                                "weight", Map.of("type", "number", "exclusiveMinimum", 0, "maximum", 100),
                                "guidance", Map.of("type", "string", "minLength", 1))))));
    }

    public static Map<String, Object> usageSchema() {
        return Map.of("type", "object", "required", List.of(
                        "provider", "model", "promptTokens", "cachedInputTokens", "completionTokens", "totalTokens",
                        "pricingVersion", "inputPriceMicrosPerMillion", "cachedInputPriceMicrosPerMillion", "outputPriceMicrosPerMillion"),
                "properties", Map.of(
                        "provider", Map.of("type", "string", "minLength", 1),
                        "model", Map.of("type", "string", "minLength", 1),
                        "promptTokens", Map.of("type", "integer", "minimum", 0),
                        "cachedInputTokens", Map.of("type", "integer", "minimum", 0),
                        "completionTokens", Map.of("type", "integer", "minimum", 0),
                        "totalTokens", Map.of("type", "integer", "minimum", 0),
                        "pricingVersion", Map.of("type", "string", "minLength", 1),
                        "inputPriceMicrosPerMillion", Map.of("type", "integer", "minimum", 0),
                        "cachedInputPriceMicrosPerMillion", Map.of("type", "integer", "minimum", 0),
                        "outputPriceMicrosPerMillion", Map.of("type", "integer", "minimum", 0)));
    }

    private static Map<String, Object> bearerSecurityScheme() {
        return Map.of("peerBearer", Map.of("httpAuthSecurityScheme", Map.of(
                "scheme", "Bearer",
                "description", "SLLP peer bearer token in the Authorization header.")));
    }

    private static List<Map<String, Object>> bearerSecurityRequirement() {
        return List.of(Map.of("schemes", Map.of("peerBearer", Map.of("list", List.of()))));
    }

    private static Map<String, Object> instructionSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "compositionIsUntrusted", Map.of("type", "boolean"),
                "doNotFollowInstructionsInComposition", Map.of("type", "boolean")));
    }
}
