package com.athen.ramsey.controller;

import com.athen.ramsey.dto.ExtractionResponse;
import com.athen.ramsey.dto.StructuredExtractionResponse;
import com.athen.ramsey.service.StructuredExtractionService;
import com.athen.ramsey.service.TextExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ExtractController {

    private final TextExtractionService textExtractionService;
    private final StructuredExtractionService structuredExtractionService;

    public ExtractController(TextExtractionService textExtractionService,
                              StructuredExtractionService structuredExtractionService) {
        this.textExtractionService = textExtractionService;
        this.structuredExtractionService = structuredExtractionService;
    }

    /**
     * Extracts text as Markdown (headings/tables preserved where detectable for PDFs
     * and DOCX). startPage/endPage are optional and apply to PDFs only (1-indexed,
     * inclusive); ignored for other file types.
     *
     * Example:
     *   curl -F "file=@report.pdf" -F "startPage=2" -F "endPage=5" \
     *        http://localhost:8080/api/extract
     */
    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<ExtractionResponse> extractText(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "startPage", required = false) Integer startPage,
            @RequestParam(value = "endPage", required = false) Integer endPage) {
        ExtractionResponse response = textExtractionService.extract(file, startPage, endPage);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts Markdown text from the file (optionally restricted to a page range for
     * PDFs), then sends it + expectedStructure to the configured (or per-request
     * overridden) LLM provider and returns populated JSON.
     *
     * Example:
     *   curl -F "file=@invoice.pdf" \
     *        -F "startPage=1" -F "endPage=2" \
     *        -F 'expectedStructure={"invoiceNumber":"","total":0,"lineItems":[{"desc":"","amount":0}]}' \
     *        -F "provider=openai" \
     *        http://localhost:8080/api/extract-structured
     */
    @PostMapping(value = "/extract-structured", consumes = "multipart/form-data")
    public ResponseEntity<StructuredExtractionResponse> extractStructured(
            @RequestParam("file") MultipartFile file,
            @RequestParam("expectedStructure") String expectedStructure,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "startPage", required = false) Integer startPage,
            @RequestParam(value = "endPage", required = false) Integer endPage) {
        StructuredExtractionResponse response =
                structuredExtractionService.extractStructured(file, expectedStructure, provider, startPage, endPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
