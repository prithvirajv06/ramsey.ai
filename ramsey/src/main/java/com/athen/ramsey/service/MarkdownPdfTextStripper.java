package com.athen.ramsey.service;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MarkdownPdfTextStripper extends PDFTextStripper {

    private final List<LineInfo> lines = new ArrayList<>();

    MarkdownPdfTextStripper() throws IOException {
        super();
        setSortByPosition(true);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (text == null || text.isBlank() || textPositions.isEmpty()) {
            return;
        }
        float avgFontSize = (float) textPositions.stream()
                .mapToDouble(TextPosition::getFontSizeInPt)
                .average()
                .orElse(0);
        lines.add(new LineInfo(text.trim(), avgFontSize));
    }

    String toMarkdown() {
        if (lines.isEmpty()) {
            return "";
        }
        double median = median(lines);
        StringBuilder md = new StringBuilder();

        int i = 0;
        while (i < lines.size()) {
            LineInfo line = lines.get(i);
            String[] cells = splitColumns(line.text());

            if (cells.length >= 2) {
                List<String[]> block = new ArrayList<>();
                block.add(cells);
                int j = i + 1;
                while (j < lines.size() && splitColumns(lines.get(j).text()).length == cells.length) {
                    block.add(splitColumns(lines.get(j).text()));
                    j++;
                }
                if (block.size() >= 2) {
                    md.append(renderTable(block)).append("\n\n");
                    i = j;
                    continue;
                }
            }

            if (line.fontSize() > median * 1.4) {
                md.append("# ").append(line.text()).append("\n\n");
            } else if (line.fontSize() > median * 1.15) {
                md.append("## ").append(line.text()).append("\n\n");
            } else {
                md.append(line.text()).append("\n");
            }
            i++;
        }
        return md.toString().trim();
    }

    private String[] splitColumns(String text) {
        return text.split(" {2,}");
    }

    private String renderTable(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        String[] header = rows.get(0);
        sb.append("| ").append(String.join(" | ", header)).append(" |\n");
        sb.append("|").append(" --- |".repeat(header.length)).append("\n");
        for (int r = 1; r < rows.size(); r++) {
            sb.append("| ").append(String.join(" | ", rows.get(r))).append(" |\n");
        }
        return sb.toString();
    }

    private double median(List<LineInfo> values) {
        List<Float> sizes = new ArrayList<>();
        for (LineInfo v : values) {
            sizes.add(v.fontSize());
        }
        Collections.sort(sizes);
        int n = sizes.size();
        if (n == 0) {
            return 0;
        }
        return n % 2 == 0 ? (sizes.get(n / 2 - 1) + sizes.get(n / 2)) / 2.0 : sizes.get(n / 2);
    }

    private record LineInfo(String text, float fontSize) {
    }
}
