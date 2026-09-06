package com.nus.iss.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nus.iss.contracts.grading.ImprovementSuggestion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** Keeps the public typed API while remaining portable across the current SQL test databases. */
@Converter
public class ImprovementSuggestionListConverter implements AttributeConverter<List<ImprovementSuggestion>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<ImprovementSuggestion>> TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<ImprovementSuggestion> suggestions) {
        if (suggestions == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(suggestions);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Unable to serialize improvement suggestions", error);
        }
    }

    @Override
    public List<ImprovementSuggestion> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(value, TYPE);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Unable to deserialize improvement suggestions", error);
        }
    }
}
