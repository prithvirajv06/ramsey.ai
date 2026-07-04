package com.athen.ramsey.service;

import com.athen.ramsey.dto.ExtractionResponse;
import com.athen.ramsey.exception.TextExtractionException;
import com.athen.ramsey.exception.UnsupportedFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    private final FileTypeDetector fileTypeDetector;
    private final PdfExtractor pdfExtractor;
    private final WordExtractor wordExtractor;
    private final OcrService ocrService;

    public TextExtractionService(FileTypeDetector fileTypeDetector,
                                  PdfExtractor pdfExtractor,
                                  WordExtractor wordExtractor,
                                  OcrService ocrService) {
        this.fileTypeDetector = fileTypeDetector;
        this.pdfExtractor = pdfExtractor;
        this.wordExtractor = wordExtractor;
        this.ocrService = ocrService;
    }

    public ExtractionResponse extract(MultipartFile file) {
        return extract(file, null, null);
    }

    public ExtractionResponse extract(MultipartFile file, Integer startPage, Integer endPage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        long start = System.currentTimeMillis();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new TextExtractionException("Could not read uploaded file bytes.", e);
        }

        DocumentType type;
        try {
            type = fileTypeDetector.detect(new ByteArrayInputStream(bytes), originalName);
        } catch (IOException e) {
            throw new TextExtractionException("Could not determine file type.", e);
        }

        String text;
        String method;
        String format = "PLAIN";
        Integer totalPages = null;
        Integer fromPage = null;
        Integer toPage = null;

        switch (type) {
            case PDF -> {
                PdfExtractor.Result result = pdfExtractor.extract(bytes, startPage, endPage);
                text = result.text();
                method = result.method();
                format = "MARKDOWN";
                totalPages = result.totalPages();
                fromPage = result.fromPage();
                toPage = result.toPage();
            }
            case DOCX -> {
                if (startPage != null || endPage != null) {
                    log.debug("startPage/endPage were provided for a .docx file and will be ignored " +
                            "(DOCX has no fixed page boundaries in the file format itself).");
                }
                text = wordExtractor.extractDocxAsMarkdown(bytes);
                method = "NATIVE";
                format = "MARKDOWN";
            }
            case DOC -> {
                text = wordExtractor.extractDoc(bytes);
                method = "NATIVE";
            }
            case IMAGE -> {
                text = ocrService.ocrImageFile(writeToTempFile(bytes, originalName));
                method = "OCR";
            }
            case PLAIN_TEXT -> {
                text = new String(bytes, StandardCharsets.UTF_8).trim();
                method = "NATIVE";
            }
            default -> throw new UnsupportedFileTypeException(
                    "Unsupported file type for '" + originalName + "'. Supported types: PDF, DOC, DOCX, images (PNG/JPG/TIFF/BMP), TXT.");
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Extracted {} chars from '{}' (type={}, method={}, format={}) in {} ms",
                text.length(), originalName, type, method, format, elapsed);

        ExtractionResponse response = new ExtractionResponse();
        response.setFileName(originalName);
        response.setDetectedType(type.name());
        response.setExtractionMethod(method);
        response.setFormat(format);
        response.setTotalPages(totalPages);
        response.setFromPage(fromPage);
        response.setToPage(toPage);
        response.setCharacterCount(text.length());
        response.setProcessingTimeMs(elapsed);
        response.setText(text);
        return response;
    }

    private File writeToTempFile(byte[] bytes, String originalName) {
        try {
            String suffix = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".img";
            Path tempFile = Files.createTempFile("upload-", suffix);
            Files.write(tempFile, bytes);
            tempFile.toFile().deleteOnExit();
            return tempFile.toFile();
        } catch (IOException e) {
            throw new TextExtractionException("Could not write temporary file for OCR processing.", e);
        }
    }
}
