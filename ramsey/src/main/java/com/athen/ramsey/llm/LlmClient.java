package com.athen.ramsey.llm;

public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);

    String modelName();
}
