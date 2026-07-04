package com.athen.ramsey.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Component
public class FileTypeDetector {

    private final Tika tika = new Tika();

    /**
     * Detects the document category by sniffing actual file content (magic bytes),
     * falling back to the file extension if content sniffing is inconclusive.
     */
    public DocumentType detect(InputStream content, String originalFileName) throws IOException {
        String mimeType = tika.detect(content, originalFileName);
        DocumentType byMime = fromMimeType(mimeType);
        if (byMime != DocumentType.UNSUPPORTED) {
            return byMime;
        }
        return fromExtension(originalFileName);
    }

    private DocumentType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return DocumentType.UNSUPPORTED;
        }
        mimeType = mimeType.toLowerCase(Locale.ROOT);

        if (mimeType.equals("application/pdf")) {
            return DocumentType.PDF;
        }
        if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return DocumentType.DOCX;
        }
        if (mimeType.equals("application/msword")) {
            return DocumentType.DOC;
        }
        if (mimeType.startsWith("image/")) {
            return DocumentType.IMAGE;
        }
        if (mimeType.equals("text/plain")) {
            return DocumentType.PLAIN_TEXT;
        }
        return DocumentType.UNSUPPORTED;
    }

    private DocumentType fromExtension(String fileName) {
        if (fileName == null) {
            return DocumentType.UNSUPPORTED;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return DocumentType.PDF;
        }
        if (lower.endsWith(".docx")) {
            return DocumentType.DOCX;
        }
        if (lower.endsWith(".doc")) {
            return DocumentType.DOC;
        }
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".tif") || lower.endsWith(".tiff") || lower.endsWith(".bmp")
                || lower.endsWith(".gif")) {
            return DocumentType.IMAGE;
        }
        if (lower.endsWith(".txt")) {
            return DocumentType.PLAIN_TEXT;
        }
        return DocumentType.UNSUPPORTED;
    }
}
