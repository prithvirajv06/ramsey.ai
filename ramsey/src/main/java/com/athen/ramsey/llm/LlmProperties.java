package com.athen.ramsey.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String provider,
        long timeoutSeconds,
        OpenAi openai,
        Gemini gemini,
        Ollama ollama
) {
    public record OpenAi(String apiKey, String baseUrl, String model, double temperature) {
    }

    public record Gemini(String apiKey, String baseUrl, String model) {
    }

    public record Ollama(String baseUrl, String model) {
    }
}
