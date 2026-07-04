package com.athen.ramsey.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.athen.ramsey.exception.TextExtractionException;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Thin wrapper around Tess4J's Tesseract instance.
 *
 * Tesseract's native instance is not guaranteed thread-safe across concurrent
 * OCR calls sharing the *same* instance, so instances are created per-call
 * (cheap relative to the OCR work itself) rather than shared as a singleton bean.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${ocr.tessdata-path}")
    private String tessdataPath;

    @Value("${ocr.language}")
    private String language;

    private ITesseract newTesseractInstance() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        // PSM 3 = fully automatic page segmentation (default, good general-purpose choice)
        tesseract.setPageSegMode(3);
        // OEM 1 = LSTM neural net engine only (best accuracy on Tesseract 4/5)
        tesseract.setOcrEngineMode(1);
        return tesseract;
    }

    public String ocrImageFile(File imageFile) {
        try {
            return newTesseractInstance().doOCR(imageFile).trim();
        } catch (TesseractException e) {
            log.error("OCR failed for file {}", imageFile.getName(), e);
            throw new TextExtractionException("OCR failed for image: " + imageFile.getName(), e);
        }
    }

    public String ocrBufferedImage(BufferedImage image) {
        try {
            return newTesseractInstance().doOCR(image).trim();
        } catch (TesseractException e) {
            log.error("OCR failed for in-memory image", e);
            throw new TextExtractionException("OCR failed for rendered page image", e);
        }
    }
}
