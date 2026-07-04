package com.athen.ramsey.service;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.athen.ramsey.exception.TextExtractionException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts text from Word documents: modern .docx (OOXML, as Markdown -
 * preserving headings/lists/tables) and legacy .doc (OLE2/HWPF, plain text).
 */
@Component
public class WordExtractor {

    private static final Logger log = LoggerFactory.getLogger(WordExtractor.class);

    public String extractDocxAsMarkdown(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             XWPFDocument document = new XWPFDocument(bis)) {
            StringBuilder md = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraph(md, paragraph);
                } else if (element instanceof XWPFTable table) {
                    appendTable(md, table);
                }
            }
            return md.toString().trim();
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

    private void appendParagraph(StringBuilder md, XWPFParagraph paragraph) {
        String text = paragraph.getText();
        if (text == null || text.isBlank()) {
            md.append("\n");
            return;
        }

        int headingLevel = headingLevel(paragraph.getStyle());
        if (headingLevel > 0) {
            md.append("#".repeat(Math.min(headingLevel, 6))).append(' ').append(text.trim()).append("\n\n");
            return;
        }

        if (paragraph.getNumID() != null) {
            md.append("- ").append(text.trim()).append("\n");
        } else {
            md.append(text.trim()).append("\n\n");
        }
    }

    private int headingLevel(String style) {
        if (style == null) {
            return 0;
        }
        String lower = style.toLowerCase();
        if (lower.equals("title")) {
            return 1;
        }
        if (lower.startsWith("heading")) {
            String digits = lower.replaceAll("\\D", "");
            if (!digits.isEmpty()) {
                try {
                    return Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
            return 1;
        }
        return 0;
    }

    private void appendTable(StringBuilder md, XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return;
        }
        for (int r = 0; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            String rowText = cells.stream()
                    .map(c -> c.getText().replace("\n", " ").trim())
                    .collect(Collectors.joining(" | "));
            md.append("| ").append(rowText).append(" |\n");
            if (r == 0) {
                md.append("|").append(" --- |".repeat(cells.size())).append("\n");
            }
        }
        md.append("\n");
    }
}
