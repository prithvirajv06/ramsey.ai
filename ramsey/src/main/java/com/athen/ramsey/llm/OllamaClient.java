package com.athen.ramsey.llm;

import com.athen.ramsey.exception.TextExtractionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;

@Component("ollama")
public class OllamaClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties.Ollama config;

    public OllamaClient(LlmProperties properties) {
        this.config = properties.ollama();
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(HttpClientFactory.withTimeout(Duration.ofSeconds(properties.timeoutSeconds())))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", config.model(),
                "prompt", systemPrompt + "\n\n" + userPrompt,
                "stream", false,
                "format", "json"
        );

        JsonNode response = restClient.post()
                .uri("/api/generate")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("response")) {
            throw new TextExtractionException("Ollama returned an empty response.");
        }
        return response.get("response").asString();
    }

    @Override
    public String modelName() {
        return config.model();
    }
}
