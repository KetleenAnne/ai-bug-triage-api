package dev.ketleen.triage.model;

import java.util.List;

/**
 * Outcome of a triage.
 *
 * @param source who produced the classification: {@code "llm"} or {@code "rules"}
 */
public record TriageResult(
        Severity severity,
        Category category,
        String suggestedTeam,
        String summary,
        List<String> labels,
        String source) {

    public static TriageResult of(Severity severity, Category category, String summary,
                                  List<String> labels, String source) {
        return new TriageResult(severity, category, category.team(), summary, List.copyOf(labels), source);
    }
}
