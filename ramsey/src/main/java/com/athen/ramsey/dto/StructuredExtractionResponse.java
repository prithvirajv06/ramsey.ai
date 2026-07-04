package com.athen.ramsey.dto;

import tools.jackson.databind.JsonNode;

public class StructuredExtractionResponse {

    private String fileName;
    private String detectedType;
    private String extractionMethod;
    private String llmProvider;
    private String llmModel;
    private long processingTimeMs;
    private JsonNode structuredData;

    public StructuredExtractionResponse() {
    }

    public StructuredExtractionResponse(String fileName, String detectedType, String extractionMethod,
                                         String llmProvider, String llmModel, long processingTimeMs,
                                         JsonNode structuredData) {
        this.fileName = fileName;
        this.detectedType = detectedType;
        this.extractionMethod = extractionMethod;
        this.llmProvider = llmProvider;
        this.llmModel = llmModel;
        this.processingTimeMs = processingTimeMs;
        this.structuredData = structuredData;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDetectedType() {
        return detectedType;
    }

    public void setDetectedType(String detectedType) {
        this.detectedType = detectedType;
    }

    public String getExtractionMethod() {
        return extractionMethod;
    }

    public void setExtractionMethod(String extractionMethod) {
        this.extractionMethod = extractionMethod;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public JsonNode getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(JsonNode structuredData) {
        this.structuredData = structuredData;
    }
}
