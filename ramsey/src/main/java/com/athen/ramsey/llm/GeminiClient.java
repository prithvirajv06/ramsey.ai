package com.athen.ramsey.llm;

import com.athen.ramsey.exception.TextExtractionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component("gemini")
public class GeminiClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties.Gemini config;

    public GeminiClient(LlmProperties properties) {
        this.config = properties.gemini();
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(HttpClientFactory.withTimeout(Duration.ofSeconds(properties.timeoutSeconds())))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(
                                Map.of("text", systemPrompt + "\n\n" + userPrompt)
                        ))
                ),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", config.model(), config.apiKey())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("candidates") || response.get("candidates").isEmpty()) {
            throw new TextExtractionException("Gemini returned an empty response.");
        }
        return response.get("candidates").get(0)
                .get("content").get("parts").get(0).get("text").asString();
    }

    @Override
    public String modelName() {
        return config.model();
    }
}
