package com.athen.ramsey.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.athen.ramsey.exception.TextExtractionException;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Extracts text from PDFs as Markdown.
 *
 * Strategy: for each page in the requested range, try layout-aware native
 * extraction first (fast, exact, and preserves headings/tables via
 * {@link MarkdownPdfTextStripper}). For any page where native extraction
 * yields little or no text (a strong signal the page is a scanned image),
 * the page is rasterized and run through Tesseract OCR instead (plain text,
 * since a scanned page carries no extractable layout metadata).
 */
@Component
public class PdfExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractor.class);

    private final OcrService ocrService;

    @Value("${ocr.pdf-render-dpi:300}")
    private int renderDpi;

    @Value("${ocr.pdf-min-text-length-per-page:20}")
    private int minTextLengthPerPage;

    public PdfExtractor(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public Result extract(byte[] pdfBytes, Integer startPage, Integer endPage) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();

            int from = (startPage == null || startPage < 1) ? 1 : startPage;
            int to = (endPage == null || endPage > totalPages) ? totalPages : endPage;

            if (from > totalPages) {
                throw new TextExtractionException(
                        "startPage " + from + " exceeds document page count (" + totalPages + ").");
            }
            if (from > to) {
                throw new TextExtractionException("startPage cannot be greater than endPage.");
            }

            StringBuilder combined = new StringBuilder();
            boolean anyOcrUsed = false;
            boolean anyNativeUsed = false;

            PDFRenderer renderer = new PDFRenderer(document);

            for (int pageNumber = from; pageNumber <= to; pageNumber++) {
                MarkdownPdfTextStripper stripper = new MarkdownPdfTextStripper();
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                stripper.getText(document);
                String pageMarkdown = stripper.toMarkdown();

                if (pageMarkdown.length() >= minTextLengthPerPage) {
                    combined.append(pageMarkdown).append("\n\n");
                    anyNativeUsed = true;
                } else {
                    log.debug("Page {} has little/no native text ({} chars) — falling back to OCR",
                            pageNumber, pageMarkdown.length());
                    BufferedImage pageImage = renderer.renderImageWithDPI(pageNumber - 1, renderDpi, ImageType.RGB);
                    String ocrText = ocrService.ocrBufferedImage(pageImage);
                    combined.append(ocrText).append("\n\n");
                    anyOcrUsed = true;
                }
            }

            String method = anyOcrUsed && anyNativeUsed ? "MIXED" : anyOcrUsed ? "OCR" : "NATIVE";
            return new Result(combined.toString().trim(), method, totalPages, from, to);

        } catch (IOException e) {
            log.error("Failed to process PDF", e);
            throw new TextExtractionException("Failed to read or process PDF file", e);
        }
    }

    public record Result(String text, String method, int totalPages, int fromPage, int toPage) {
    }
}
