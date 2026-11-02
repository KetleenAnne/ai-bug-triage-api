package dev.ketleen.triage.classifier;

import dev.ketleen.triage.model.TriageRequest;
import dev.ketleen.triage.model.TriageResult;

public interface TriageClassifier {

    TriageResult classify(TriageRequest request);
}
