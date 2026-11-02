package dev.ketleen.triage.api;

import dev.ketleen.triage.classifier.TriageClassifier;
import dev.ketleen.triage.model.TriageRequest;
import dev.ketleen.triage.model.TriageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TriageController {

    private final TriageClassifier classifier;

    public TriageController(TriageClassifier classifier) {
        this.classifier = classifier;
    }

    @PostMapping("/triage")
    public TriageResult triage(@Valid @RequestBody TriageRequest request) {
        return classifier.classify(request);
    }
}
