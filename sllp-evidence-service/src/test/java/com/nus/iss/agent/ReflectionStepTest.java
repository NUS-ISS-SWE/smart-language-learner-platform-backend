package com.nus.iss.agent;

import com.nus.iss.enums.EvidencePolarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionStepTest {

    private static final String COMPOSITION = """
            The sun rose slowly over the quiet village. Birds sang in the trees, and the smell
            of fresh bread drifted from the bakery down the street.""";

    @Test
    void acceptsExcerptsThatAreRealSubstrings() {
        EvidenceAgentResponse response = new EvidenceAgentResponse(
                List.of("plan step"),
                List.of(new EvidenceAgentResponse.CriterionFinding("Imagery", List.of(
                        new EvidenceAgentResponse.ExcerptFinding(
                                "The sun rose slowly over the quiet village.", EvidencePolarity.STRENGTH, "vivid opening image")))));

        assertTrue(ReflectionStep.findInvalidExcerpts(response, COMPOSITION).isEmpty());
    }

    @Test
    void flagsExcerptsThatWereNotFoundVerbatim() {
        EvidenceAgentResponse response = new EvidenceAgentResponse(
                List.of("plan step"),
                List.of(new EvidenceAgentResponse.CriterionFinding("Imagery", List.of(
                        new EvidenceAgentResponse.ExcerptFinding(
                                "The sun rose quickly over the busy city.", EvidencePolarity.WEAKNESS, "not from the original text")))));

        List<String> invalid = ReflectionStep.findInvalidExcerpts(response, COMPOSITION);

        assertEquals(1, invalid.size());
        assertTrue(invalid.get(0).contains("Imagery"));
    }

    @Test
    void flagsBlankOrMissingExcerpts() {
        EvidenceAgentResponse response = new EvidenceAgentResponse(
                List.of("plan step"),
                List.of(new EvidenceAgentResponse.CriterionFinding("Imagery", List.of(
                        new EvidenceAgentResponse.ExcerptFinding(null, EvidencePolarity.WEAKNESS, "missing"),
                        new EvidenceAgentResponse.ExcerptFinding("   ", EvidencePolarity.WEAKNESS, "blank")))));

        assertEquals(2, ReflectionStep.findInvalidExcerpts(response, COMPOSITION).size());
    }

    @Test
    void locatesVerbatimEvidenceInTheCorrectHtmlParagraph() {
        String content = "<p>First paragraph.</p><p>The second paragraph has the evidence.</p>";

        assertEquals("paragraph 2, characters 27-64",
                ReflectionStep.sourceLocation(content, "The second paragraph has the evidence."));
    }
}
