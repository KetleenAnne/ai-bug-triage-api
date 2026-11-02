package dev.ketleen.triage.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls the Anthropic Messages API using only the JDK HTTP client. */
public class AnthropicLlmClient implements LlmClient {

    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AnthropicLlmClient(String apiKey, String model, String baseUrl, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.mapper = mapper;
    }

    @Override
    public String complete(String system, String user) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 400);
            body.put("system", system);
            ArrayNode messages = body.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", user);

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                    .timeout(Duration.ofSeconds(30))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // Never include the body or the key in the message: they may hold user data.
                throw new LlmException("LLM answered with HTTP " + response.statusCode());
            }

            JsonNode text = mapper.readTree(response.body()).path("content").path(0).path("text");
            if (!text.isTextual()) {
                throw new LlmException("LLM response has no text content");
            }
            return text.asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM call interrupted", e);
        } catch (JsonProcessingException e) {
            throw new LlmException("Could not read the LLM response", e);
        } catch (IOException e) {
            throw new LlmException("Could not reach the LLM", e);
        }
    }
}
