package com.athen.ramsey.llm;

import com.athen.ramsey.exception.TextExtractionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component("openai")
public class OpenAiClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties.OpenAi config;

    public OpenAiClient(LlmProperties properties) {
        this.config = properties.openai();
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .requestFactory(HttpClientFactory.withTimeout(Duration.ofSeconds(properties.timeoutSeconds())))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", config.model(),
                "temperature", config.temperature(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
            throw new TextExtractionException("OpenAI returned an empty response.");
        }
        return response.get("choices").get(0).get("message").get("content").asString();
    }

    @Override
    public String modelName() {
        return config.model();
    }
}
