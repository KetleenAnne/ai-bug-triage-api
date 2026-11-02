package dev.ketleen.triage.classifier;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ketleen.triage.model.Category;
import dev.ketleen.triage.model.Severity;
import dev.ketleen.triage.model.TriageRequest;
import dev.ketleen.triage.model.TriageResult;
import org.junit.jupiter.api.Test;

class RuleBasedClassifierTest {

    private final RuleBasedClassifier classifier = new RuleBasedClassifier();

    private TriageResult run(String title, String description) {
        return classifier.classify(new TriageRequest(title, description, null));
    }

    @Test
    void loginBrokenIsCriticalFunctional() {
        TriageResult r = run("Users cannot login after deploy", "Everyone gets rejected.");
        assertThat(r.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(r.category()).isEqualTo(Category.FUNCTIONAL);
        assertThat(r.suggestedTeam()).isEqualTo("backend");
    }

    @Test
    void securityIsNeverBelowHigh() {
        TriageResult r = run("Possible XSS on comment field", "Script tags are rendered.");
        assertThat(r.category()).isEqualTo(Category.SECURITY);
        assertThat(r.severity().ordinal()).isLessThanOrEqualTo(Severity.HIGH.ordinal());
        assertThat(r.suggestedTeam()).isEqualTo("security");
    }

    @Test
    void typoIsLowUi() {
        TriageResult r = run("Typo in footer", "Word is misspelled.");
        assertThat(r.category()).isEqualTo(Category.UI_UX);
        assertThat(r.severity()).isEqualTo(Severity.LOW);
    }

    @Test
    void slowReportIsHighPerformance() {
        TriageResult r = run("Report export is very slow", "The request hits a timeout.");
        assertThat(r.category()).isEqualTo(Category.PERFORMANCE);
        assertThat(r.severity()).isEqualTo(Severity.HIGH);
        assertThat(r.suggestedTeam()).isEqualTo("platform");
    }

    @Test
    void duplicateRecordsAreMediumData() {
        TriageResult r = run("Duplicate records created", "Each save inserts two rows.");
        assertThat(r.category()).isEqualTo(Category.DATA);
        assertThat(r.severity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void webhookFailureIsIntegration() {
        TriageResult r = run("Webhook fails on third-party callback", "We receive nothing.");
        assertThat(r.category()).isEqualTo(Category.INTEGRATION);
        assertThat(r.severity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void unknownTextFallsBackToOtherMedium() {
        TriageResult r = run("Question about pricing", "How much is the premium plan?");
        assertThat(r.category()).isEqualTo(Category.OTHER);
        assertThat(r.severity()).isEqualTo(Severity.MEDIUM);
        assertThat(r.source()).isEqualTo("rules");
    }

    @Test
    void longTitleIsTruncatedInSummary() {
        TriageResult r = run("x".repeat(200), "details");
        assertThat(r.summary().length()).isLessThanOrEqualTo(120);
    }
}
