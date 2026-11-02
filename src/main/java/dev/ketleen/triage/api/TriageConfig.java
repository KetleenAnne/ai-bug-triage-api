package dev.ketleen.triage.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ketleen.triage.classifier.LlmTriageClassifier;
import dev.ketleen.triage.classifier.RuleBasedClassifier;
import dev.ketleen.triage.classifier.TriageClassifier;
import dev.ketleen.triage.llm.AnthropicLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TriageConfig {

    @Bean
    TriageClassifier triageClassifier(
            @Value("${triage.provider:rules}") String provider,
            @Value("${triage.anthropic.api-key:}") String apiKey,
            @Value("${triage.anthropic.model:claude-sonnet-5-5}") String model,
            @Value("${triage.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            ObjectMapper mapper) {

        RuleBasedClassifier rules = new RuleBasedClassifier();

        if ("anthropic".equalsIgnoreCase(provider)) {
            if (apiKey.isBlank()) {
                throw new IllegalStateException(
                        "triage.provider=anthropic requires the ANTHROPIC_API_KEY environment variable");
            }
            return new LlmTriageClassifier(new AnthropicLlmClient(apiKey, model, baseUrl, mapper), rules, mapper);
        }
        return rules;
    }
}
