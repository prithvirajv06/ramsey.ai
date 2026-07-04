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

    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<ExtractionResponse> extractText(@RequestParam("file") MultipartFile file) {
        ExtractionResponse response = textExtractionService.extract(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts text from the file, then sends it + expectedStructure to the
     * configured (or per-request overridden) LLM provider and returns populated JSON.
     *
     * Example:
     *   curl -F "file=@invoice.pdf" \
     *        -F 'expectedStructure={"invoiceNumber":"","total":0,"lineItems":[{"desc":"","amount":0}]}' \
     *        -F "provider=openai" \
     *        http://localhost:8080/api/extract-structured
     */
    @PostMapping(value = "/extract-structured", consumes = "multipart/form-data")
    public ResponseEntity<StructuredExtractionResponse> extractStructured(
            @RequestParam("file") MultipartFile file,
            @RequestParam("expectedStructure") String expectedStructure,
            @RequestParam(value = "provider", required = false) String provider) {
        StructuredExtractionResponse response =
                structuredExtractionService.extractStructured(file, expectedStructure, provider);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
