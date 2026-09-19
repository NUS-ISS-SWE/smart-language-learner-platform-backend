package com.nus.iss.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nus.iss.contracts.grading.ImprovementSuggestion;

class ImprovementSuggestionListConverterTest {

    private final ImprovementSuggestionListConverter converter = new ImprovementSuggestionListConverter();

    @Test
    void roundTripsExistingSuggestionJson() {
        String storedJson = """
                [{"criterion":"Grammar","priority":"HIGH","recommendation":"Use past tense consistently","exampleRevision":null,"learningTip":null,"supportingEvidence":null}]
                """;

        List<ImprovementSuggestion> suggestions = converter.convertToEntityAttribute(storedJson);

        assertThat(suggestions).containsExactly(new ImprovementSuggestion(
                "Grammar", "HIGH", "Use past tense consistently", null, null, null));
        assertThat(converter.convertToDatabaseColumn(suggestions)).isEqualTo(storedJson.strip());
    }
}
