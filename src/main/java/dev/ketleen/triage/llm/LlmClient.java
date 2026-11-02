package dev.ketleen.triage.llm;

/** Minimal port to a language model. Lets the classifier be tested without any network. */
public interface LlmClient {

    /** @throws LlmException when the model cannot be reached or answers with an error */
    String complete(String system, String user);
}
