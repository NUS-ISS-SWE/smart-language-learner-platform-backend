package com.nus.iss.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guards against hallucinated evidence: every excerpt the model returns must be a verbatim
 * substring of the actual composition. Anything that isn't gets listed here so
 * {@link com.nus.iss.agent.EvidenceAgentService} can ask the model to correct it.
 */
public final class ReflectionStep {
    private static final Pattern HTML_PARAGRAPH = Pattern.compile("<p\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private ReflectionStep() {
    }

    public static List<String> findInvalidExcerpts(EvidenceAgentResponse response, String content) {
        List<String> invalid = new ArrayList<>();
        if (response.criteria() == null || content == null) {
            return invalid;
        }
        for (EvidenceAgentResponse.CriterionFinding criterion : response.criteria()) {
            if (criterion.evidence() == null) {
                continue;
            }
            for (EvidenceAgentResponse.ExcerptFinding item : criterion.evidence()) {
                String excerpt = item.excerpt();
                if (excerpt == null || excerpt.isBlank() || !content.contains(excerpt.trim())) {
                    invalid.add(criterion.criterion() + ": \"" + excerpt + "\"");
                }
            }
        }
        return invalid;
    }

    /** Returns the stable learner-document location for a verbatim excerpt, or {@code null}. */
    public static String sourceLocation(String content, String excerpt) {
        if (content == null || excerpt == null || excerpt.isBlank()) return null;
        String trimmed = excerpt.trim();
        int start = content.indexOf(trimmed);
        if (start < 0) return null;
        Matcher paragraphs = HTML_PARAGRAPH.matcher(content.substring(0, start));
        int paragraph = 0;
        while (paragraphs.find()) paragraph++;
        if (paragraph == 0) paragraph = 1 + countPlainTextParagraphs(content, start);
        return "paragraph " + paragraph + ", characters " + (start + 1) + "-" + (start + trimmed.length());
    }

    private static int countPlainTextParagraphs(String content, int endExclusive) {
        int paragraphs = 0;
        for (int index = 0; index + 1 < endExclusive; index++) {
            if (content.charAt(index) == '\n' && content.charAt(index + 1) == '\n') paragraphs++;
        }
        return paragraphs;
    }
}
