package dev.ketleen.triage.classifier;

import dev.ketleen.triage.model.Category;
import dev.ketleen.triage.model.Severity;
import dev.ketleen.triage.model.TriageRequest;
import dev.ketleen.triage.model.TriageResult;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Deterministic keyword classifier. It is the default provider (no API key needed) and
 * also the fallback when the LLM is unavailable or answers with something invalid.
 */
public class RuleBasedClassifier implements TriageClassifier {

    private static final Pattern SECURITY = any("vulnerab", "xss", "sql injection", "injection",
            "unauthorized", "csrf", "cve", "exposed", "data leak", "credential", "vazamento", "sem autoriza");
    private static final Pattern DATA = any("data loss", "corrupt", "duplicate", "wrong data",
            "missing data", "perda de dados", "duplicad");
    private static final Pattern PERFORMANCE = any("slow", "timeout", "latency", "performance",
            "lento", "lentid", "memory", "high cpu");
    private static final Pattern INTEGRATION = any("webhook", "integration", "third-party",
            "integra", "callback");
    private static final Pattern UI = any("typo", "button", "layout", "css", "alignment",
            "footer", "screen", "tela", "bot\u00e3o");
    private static final Pattern FUNCTIONAL = any("cannot", "can't", "fail", "error", "broken",
            "exception", "crash", "bug", "não funciona", "erro");

    private static final Pattern CRITICAL = any("data loss", "production down", "outage", "crash",
            "security breach", "cannot login", "can't login", "payment", "perda de dados", "fora do ar");
    private static final Pattern HIGH = any("error 500", "exception", "fail", "broken", "unable",
            "cannot", "timeout", "erro 500");
    private static final Pattern LOW = any("typo", "cosmetic", "minor", "alignment", "suggestion",
            "sugest");

    @Override
    public TriageResult classify(TriageRequest request) {
        String text = request.title() + "\n" + request.description() + "\n"
                + (request.environment() == null ? "" : request.environment());

        Category category = category(text);
        Severity severity = severity(text, category);

        return TriageResult.of(
                severity,
                category,
                summary(request.title()),
                List.of(category.name().toLowerCase(), "severity-" + severity.name().toLowerCase()),
                "rules");
    }

    private static Category category(String text) {
        if (SECURITY.matcher(text).find()) return Category.SECURITY;
        if (DATA.matcher(text).find()) return Category.DATA;
        if (PERFORMANCE.matcher(text).find()) return Category.PERFORMANCE;
        if (INTEGRATION.matcher(text).find()) return Category.INTEGRATION;
        if (UI.matcher(text).find()) return Category.UI_UX;
        if (FUNCTIONAL.matcher(text).find()) return Category.FUNCTIONAL;
        return Category.OTHER;
    }

    private static Severity severity(String text, Category category) {
        Severity severity;
        if (CRITICAL.matcher(text).find()) {
            severity = Severity.CRITICAL;
        } else if (LOW.matcher(text).find()) {
            severity = Severity.LOW;
        } else if (HIGH.matcher(text).find()) {
            severity = Severity.HIGH;
        } else {
            severity = Severity.MEDIUM;
        }
        // Security issues are never below HIGH.
        if (category == Category.SECURITY && severity.ordinal() > Severity.HIGH.ordinal()) {
            severity = Severity.HIGH;
        }
        return severity;
    }

    private static String summary(String title) {
        String t = title.strip();
        return t.length() <= 120 ? t : t.substring(0, 117) + "...";
    }

    /** Case-insensitive, matches terms at the start of a word (so "fail" also matches "fails"). */
    private static Pattern any(String... terms) {
        StringBuilder sb = new StringBuilder("\\b(?:");
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(Pattern.quote(terms[i]));
        }
        sb.append(')');
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
