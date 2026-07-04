package com.athen.ramsey.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.athen.ramsey.exception.TextExtractionException;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Extracts text from PDFs.
 * <p>
 * Strategy: try native text extraction per page first (fast, exact — works for
 * PDFs that contain a real text layer). For any page where native extraction
 * yields little or no text (a strong signal the page is a scanned image),
 * the page is rasterized and run through Tesseract OCR instead.
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

    public Result extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            StringBuilder combined = new StringBuilder();
            boolean anyOcrUsed = false;
            boolean anyNativeUsed = false;

            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);

            stripper.setStartPage(0);
            stripper.setEndPage(5);
            String nativeText = stripper.getText(document).trim();

            if (nativeText.length() >= minTextLengthPerPage) {
                combined.append(nativeText).append("\n\n");
                anyNativeUsed = true;
            } else {
                log.debug("Page {} has little/no native text ({} chars) — falling back to OCR",
                        0, nativeText.length());
                BufferedImage pageImage = renderer.renderImageWithDPI(0, renderDpi, ImageType.RGB);
                String ocrText = ocrService.ocrBufferedImage(pageImage);
                combined.append(ocrText).append("\n\n");
                anyOcrUsed = true;
            }

            String method = anyOcrUsed && anyNativeUsed ? "MIXED" : anyOcrUsed ? "OCR" : "NATIVE";
            return new Result(combined.toString().trim(), method, pageCount);

        } catch (IOException e) {
            log.error("Failed to process PDF", e);
            throw new TextExtractionException("Failed to read or process PDF file", e);
        }
    }

    public record Result(String text, String method, int pageCount) {
    }
}
