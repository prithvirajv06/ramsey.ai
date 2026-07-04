# text-extract-service

A Spring Boot microservice that accepts **PDF, Word (.doc/.docx), image (PNG/JPG/TIFF/BMP), and plain text**
files, and returns the extracted text — using native text extraction where possible
(PDFBox / Apache POI) and falling back to **Tesseract OCR** (via Tess4J) for scanned
pages or images.

## How file types are handled

| Type              | Method                                                                 |
|-------------------|-------------------------------------------------------------------------|
| `.pdf`            | Native text extraction per page (PDFBox). Pages with little/no embedded text (scanned pages) are rasterized and OCR'd with Tesseract. Mixed PDFs work page-by-page. |
| `.docx`           | Native extraction via Apache POI (XWPF).                               |
| `.doc` (legacy)   | Native extraction via Apache POI (HWPF).                               |
| `.png/.jpg/.tiff/.bmp/.gif` | Full-page OCR via Tesseract.                                  |
| `.txt`            | Read as UTF-8 text.                                                    |

File type is detected by **content sniffing** (Apache Tika, magic bytes) rather than
trusting the file extension, with the extension used only as a fallback.

## Prerequisites

1. **Java 17+** and **Maven 3.9+**
2. **Tesseract OCR engine installed natively** on the host (Tess4J is a JNA wrapper
   around the native `libtesseract` — it does not bundle the engine itself):

   ```bash
   # Ubuntu/Debian
   sudo apt-get install tesseract-ocr tesseract-ocr-eng libtesseract-dev libleptonica-dev

   # macOS
   brew install tesseract

   # Windows
   # Install from https://github.com/UB-Mannheim/tesseract/wiki
   ```

3. Locate your `tessdata` directory (contains `eng.traineddata`, etc.) — commonly:
   - Linux: `/usr/share/tesseract-ocr/5/tessdata` or `/usr/share/tessdata`
   - macOS (Homebrew): `/opt/homebrew/share/tessdata`
   - Windows: `C:\Program Files\Tesseract-OCR\tessdata`

   Set it via the `TESSDATA_PREFIX` env var, or edit `application.properties`.

## Running locally

```bash
export TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata   # adjust to your system
mvn clean package
java -jar target/text-extract-service.jar
```

The service starts on **http://localhost:8080**.

## Running with Docker (recommended — Tesseract is bundled)

```bash
docker build -t text-extract-service .
docker run -p 8080:8080 text-extract-service
# Docker Compose
docker compose up --build
```

## API

### `POST /api/extract`

Multipart form upload, field name `file`.

```bash
curl -F "file=@invoice_scan.pdf" http://localhost:8080/api/extract
curl -F "file=@contract.docx"    http://localhost:8080/api/extract
curl -F "file=@receipt.jpg"      http://localhost:8080/api/extract
```

**Response:**

```json
{
  "fileName": "invoice_scan.pdf",
  "detectedType": "PDF",
  "extractionMethod": "OCR",
  "characterCount": 1423,
  "processingTimeMs": 842,
  "text": "INVOICE\nDate: 2026-05-11\n..."
}
```

- `extractionMethod` is one of `NATIVE`, `OCR`, or `MIXED` (PDFs with some text
  pages and some scanned pages).

### `GET /api/health`

Simple liveness check, returns `OK`.

## Configuration (`application.properties`)

| Property | Default | Purpose |
|---|---|---|
| `ocr.tessdata-path` | `${TESSDATA_PREFIX}` | Path to Tesseract's trained data files |
| `ocr.language` | `eng` | Tesseract language(s), e.g. `eng+fra` |
| `ocr.pdf-render-dpi` | `300` | DPI used when rasterizing scanned PDF pages before OCR |
| `ocr.pdf-min-text-length-per-page` | `20` | Below this many native chars, a PDF page is treated as scanned and OCR'd |
| `spring.servlet.multipart.max-file-size` | `50MB` | Max upload size |

## Notes / extension points

- **Large files / async**: for very large batches, consider making `/api/extract`
  return a job ID and processing asynchronously (e.g. with Spring's `@Async` or a
  queue) rather than blocking the HTTP request.
- **Additional languages**: install extra `tesseract-ocr-<lang>` packages and set
  `ocr.language=eng+fra` (etc.).
- **Other formats** (PPTX, XLS, RTF, HTML): Apache POI/Tika already cover most of
  these — extend `FileTypeDetector` and add an extractor following the same pattern
  as `WordExtractor`.
