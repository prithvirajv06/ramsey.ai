package com.athen.ramsey.service;

import com.athen.ramsey.dto.ExtractionResponse;
import com.athen.ramsey.dto.StructuredExtractionResponse;
import com.athen.ramsey.exception.TextExtractionException;
import com.athen.ramsey.llm.LlmClient;
import com.athen.ramsey.llm.LlmClientResolver;
import com.athen.ramsey.llm.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class StructuredExtractionService {

    private static final Logger log = LoggerFactory.getLogger(StructuredExtractionService.class);

    private static final String SYSTEM_PROMPT = """
            You are a precise data extraction engine. You will be given raw text extracted \
            from a document and a target JSON structure. Return ONLY a single valid JSON \
            object that matches the target structure exactly - same keys, same nesting, same \
            array/object shape. Populate values using information found in the text. If a \
            value cannot be found, use null. Do not add extra keys. Do not include any \
            explanation, commentary, or markdown code fences - output raw JSON only.
            Extract details based on the description provided in the JSON template.
            """;

    private final TextExtractionService textExtractionService;
    private final LlmClientResolver llmClientResolver;
    private final LlmProperties llmProperties;
    private final JsonMapper jsonMapper;

    public StructuredExtractionService(TextExtractionService textExtractionService,
                                        LlmClientResolver llmClientResolver,
                                        LlmProperties llmProperties,
                                        JsonMapper jsonMapper) {
        this.textExtractionService = textExtractionService;
        this.llmClientResolver = llmClientResolver;
        this.llmProperties = llmProperties;
        this.jsonMapper = jsonMapper;
    }

    public StructuredExtractionResponse extractStructured(MultipartFile file, String expectedStructure,
                                                            String providerOverride) {
        if (expectedStructure == null || expectedStructure.isBlank()) {
            throw new IllegalArgumentException("expectedStructure must be provided and contain a JSON shape.");
        }

        long start = System.currentTimeMillis();

        ExtractionResponse extraction = textExtractionService.extract(file);
        String effectiveProvider = (providerOverride != null && !providerOverride.isBlank())
                ? providerOverride.toLowerCase()
                : llmProperties.provider().toLowerCase();
        LlmClient client = llmClientResolver.resolve(providerOverride);

        String userPrompt = "TARGET JSON STRUCTURE:\n" + expectedStructure
                + "\n\nEXTRACTED DOCUMENT TEXT:\n" + extraction.getText();

        String rawLlmOutput = client.complete(SYSTEM_PROMPT, userPrompt);
        JsonNode structuredData = parseJson(rawLlmOutput);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Structured extraction for '{}' via provider={} model={} completed in {} ms",
                extraction.getFileName(), effectiveProvider, client.modelName(), elapsed);

        return new StructuredExtractionResponse(
                extraction.getFileName(),
                extraction.getDetectedType(),
                extraction.getExtractionMethod(),
                effectiveProvider,
                client.modelName(),
                elapsed,
                structuredData
        );
    }

    private JsonNode parseJson(String rawOutput) {
        String cleaned = stripCodeFences(rawOutput);
        try {
            return jsonMapper.readTree(cleaned);
        } catch (JacksonException e) {
            log.error("LLM did not return valid JSON: {}", rawOutput);
            throw new TextExtractionException("LLM response was not valid JSON: " + e.getMessage(), e);
        }
    }

    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
