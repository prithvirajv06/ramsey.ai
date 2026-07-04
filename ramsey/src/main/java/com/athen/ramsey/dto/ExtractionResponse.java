package com.athen.ramsey.dto;

public class ExtractionResponse {

    private String fileName;
    private String detectedType;
    private String extractionMethod; // e.g. "NATIVE", "OCR", "MIXED"
    private int characterCount;
    private long processingTimeMs;
    private String text;

    public ExtractionResponse() {
    }

    public ExtractionResponse(String fileName, String detectedType, String extractionMethod,
                               int characterCount, long processingTimeMs, String text) {
        this.fileName = fileName;
        this.detectedType = detectedType;
        this.extractionMethod = extractionMethod;
        this.characterCount = characterCount;
        this.processingTimeMs = processingTimeMs;
        this.text = text;
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

    public int getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(int characterCount) {
        this.characterCount = characterCount;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
