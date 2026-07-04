package com.athen.ramsey.llm;

import com.athen.ramsey.exception.TextExtractionException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LlmClientResolver {

    private final Map<String, LlmClient> clientsByName;
    private final LlmProperties properties;

    public LlmClientResolver(Map<String, LlmClient> clientsByName, LlmProperties properties) {
        this.clientsByName = clientsByName;
        this.properties = properties;
    }

    public LlmClient resolve(String requestedProvider) {
        String key = (requestedProvider != null && !requestedProvider.isBlank()
                ? requestedProvider
                : properties.provider()).toLowerCase();

        LlmClient client = clientsByName.get(key);
        if (client == null) {
            throw new TextExtractionException(
                    "Unknown LLM provider '" + key + "'. Supported: " + clientsByName.keySet());
        }
        return client;
    }
}
