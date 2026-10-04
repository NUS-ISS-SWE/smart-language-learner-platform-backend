package com.nus.iss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiStatusResponse(Summary summary) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            @JsonProperty("affected_components") List<Object> affectedComponents,
            @JsonProperty("ongoing_incidents") List<Object> ongoingIncidents) {
    }
}
