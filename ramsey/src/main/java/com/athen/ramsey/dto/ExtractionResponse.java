package com.athen.ramsey.dto;

public class ExtractionResponse {

    private String fileName;
    private String detectedType;
    private String extractionMethod;
    private String format;
    private Integer totalPages;
    private Integer fromPage;
    private Integer toPage;
    private int characterCount;
    private long processingTimeMs;
    private String text;

    public ExtractionResponse() {
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getFromPage() {
        return fromPage;
    }

    public void setFromPage(Integer fromPage) {
        this.fromPage = fromPage;
    }

    public Integer getToPage() {
        return toPage;
    }

    public void setToPage(Integer toPage) {
        this.toPage = toPage;
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
