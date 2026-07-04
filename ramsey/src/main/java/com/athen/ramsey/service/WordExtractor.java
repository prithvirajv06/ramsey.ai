package com.athen.ramsey.service;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.athen.ramsey.exception.TextExtractionException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extracts text from Word documents: modern .docx (OOXML) and legacy .doc (OLE2/HWPF).
 */
@Component
public class WordExtractor {

    private static final Logger log = LoggerFactory.getLogger(WordExtractor.class);

    public String extractDocx(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             XWPFDocument document = new XWPFDocument(bis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText().trim();
        } catch (IOException e) {
            log.error("Failed to process .docx file", e);
            throw new TextExtractionException("Failed to read or process .docx file", e);
        }
    }

    public String extractDoc(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             org.apache.poi.hwpf.extractor.WordExtractor extractor =
                     new org.apache.poi.hwpf.extractor.WordExtractor(bis)) {
            return extractor.getText().trim();
        } catch (IOException e) {
            log.error("Failed to process .doc file", e);
            throw new TextExtractionException("Failed to read or process .doc file", e);
        }
    }
}
