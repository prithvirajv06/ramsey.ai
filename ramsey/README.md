# Ramsey.ai

A Spring Boot microservice that accepts **PDF, Word (.doc/.docx), image (PNG/JPG/TIFF/BMP), and plain text**
files, and returns the extracted content as **Markdown** — preserving headings and table
structure where possible, so the output is easy for an LLM (or a human) to relate back to
the original layout — using native text extraction where possible (PDFBox / Apache POI)
and falling back to **Tesseract OCR** (via Tess4J) for scanned pages or images.

## How file types are handled

| Type              | Output format | Method                                                                 |
|-------------------|---------------|-------------------------------------------------------------------------|
| `.pdf`            | Markdown      | Layout-aware extraction per page (PDFBox): font-size is used to detect headings (`#`/`##`), and aligned multi-column lines are rendered as Markdown tables. Pages with little/no embedded text (scanned pages) are rasterized and OCR'd with Tesseract instead (plain text for that page). Mixed PDFs work page-by-page. Supports a `startPage`/`endPage` range. |
| `.docx`           | Markdown      | Native extraction via Apache POI (XWPF): heading styles → `#`/`##`, list paragraphs → `-`, and `XWPFTable`s → Markdown tables. |
| `.doc` (legacy)   | Plain text    | Native extraction via Apache POI (HWPF) — legacy binary format, structural Markdown conversion isn't supported. |
| `.png/.jpg/.tiff/.bmp/.gif` | Plain text | Full-page OCR via Tesseract — OCR has no layout/table awareness. |
| `.txt`            | Plain text    | Read as UTF-8 text.                                                    |

The Markdown conversion is heuristic-based (font size for headings, column alignment for
tables) — it's a strong best-effort for well-structured documents, not a guaranteed
pixel-perfect layout reconstruction.

File type is detected by **content sniffing** (Apache Tika, magic bytes) rather than
trusting the file extension, with the extension used only as a fallback.

## Prerequisites

1. **Java 25+** and **Gradle 8.14+** (or Gradle 9)
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
   - Linux: `/usr/share/tesseract-ocr/4.00/tessdata` (Tesseract 4.x) or `/usr/share/tesseract-ocr/5/tessdata` (5.x)
   - macOS (Homebrew): `/opt/homebrew/share/tessdata`
   - Windows: `C:\Program Files\Tesseract-OCR\tessdata`

   Set it via the `TESSDATA_PREFIX` env var, or edit `application.properties`. When running
   via Docker, `entrypoint.sh` auto-detects this for you.

## Running locally

```bash
export TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata   # adjust to your system
./gradlew clean bootJar
java -jar build/libs/ramsey.jar
```

The service starts on **http://localhost:8080**.

## Running with Docker (recommended — Tesseract is bundled)

```bash
docker compose up --build
```

## API

### `POST /api/extract`

Multipart form upload. `startPage`/`endPage` are optional, 1-indexed, inclusive, and
apply to PDFs only (ignored for other file types).

```bash
curl -F "file=@invoice_scan.pdf" http://localhost:8080/api/extract
curl -F "file=@contract.docx"    http://localhost:8080/api/extract
curl -F "file=@receipt.jpg"      http://localhost:8080/api/extract
curl -F "file=@report.pdf" -F "startPage=2" -F "endPage=5" http://localhost:8080/api/extract
```

**Response:**

```json
{
  "fileName": "invoice_scan.pdf",
  "detectedType": "PDF",
  "extractionMethod": "OCR",
  "format": "MARKDOWN",
  "totalPages": 5,
  "fromPage": 1,
  "toPage": 5,
  "characterCount": 1423,
  "processingTimeMs": 842,
  "text": "# INVOICE\n\n| Item | Qty | Price |\n| --- | --- | --- |\n| Widget | 2 | 9.99 |\n..."
}
```

- `extractionMethod` is one of `NATIVE`, `OCR`, or `MIXED` (PDFs with some text
  pages and some scanned pages).
- `format` is `MARKDOWN` for PDF/DOCX, `PLAIN` for legacy DOC, images, and TXT.
- `totalPages`/`fromPage`/`toPage` are only populated for PDFs; `null` otherwise.

### `GET /api/health`

Simple liveness check, returns `OK`.

### `POST /api/extract-structured`

Extracts text from the file, then sends the text plus a target JSON shape you
provide to the configured LLM (OpenAI, Gemini, or Ollama), and returns the
shape populated with values found in the document. The target shape can be
any nesting/complexity — objects, arrays, arrays of objects, etc.

Fields:
- `file` — the document (multipart)
- `expectedStructure` — a JSON string describing the shape you want back
- `provider` *(optional)* — `openai`, `gemini`, or `ollama`; overrides `llm.provider` for this one call
- `startPage`/`endPage` *(optional)* — restrict extraction to a page range (PDFs only)

```bash
curl -F "file=@invoice.pdf" \
     -F 'expectedStructure={"invoiceNumber":"","invoiceDate":"","total":0,"lineItems":[{"description":"","amount":0}]}' \
     -F "provider=openai" \
     -F "startPage=1" -F "endPage=1" \
     http://localhost:8080/api/extract-structured
```

**Response:**

```json
{
  "fileName": "invoice.pdf",
  "detectedType": "PDF",
  "extractionMethod": "NATIVE",
  "llmProvider": "openai",
  "llmModel": "gpt-4o-mini",
  "processingTimeMs": 1310,
  "structuredData": {
    "invoiceNumber": "INV-1042",
    "invoiceDate": "2026-05-11",
    "total": 482.50,
    "lineItems": [
      { "description": "Consulting services", "amount": 400.00 },
      { "description": "Travel expenses", "amount": 82.50 }
    ]
  }
}
```

## Configuration (`application.properties`)

| Property | Default | Purpose |
|---|---|---|
| `ocr.tessdata-path` | `${TESSDATA_PREFIX}` | Path to Tesseract's trained data files |
| `ocr.language` | `eng` | Tesseract language(s), e.g. `eng+fra` |
| `ocr.pdf-render-dpi` | `300` | DPI used when rasterizing scanned PDF pages before OCR |
| `ocr.pdf-min-text-length-per-page` | `20` | Below this many native chars, a PDF page is treated as scanned and OCR'd |
| `spring.servlet.multipart.max-file-size` | `50MB` | Max upload size |
| `llm.provider` | `openai` | Default LLM provider: `openai`, `gemini`, or `ollama` |
| `llm.timeout-seconds` | `60` | HTTP timeout for LLM calls |
| `llm.openai.api-key` | — | Set via `OPENAI_API_KEY` |
| `llm.openai.base-url` | `https://api.openai.com/v1` | OpenAI-compatible base URL |
| `llm.openai.model` | `gpt-4o-mini` | Set via `OPENAI_MODEL` |
| `llm.openai.temperature` | `0.2` | Set via `OPENAI_TEMPERATURE` |
| `llm.gemini.api-key` | — | Set via `GEMINI_API_KEY` |
| `llm.gemini.base-url` | `https://generativelanguage.googleapis.com/v1beta` | Gemini base URL |
| `llm.gemini.model` | `gemini-1.5-flash` | Set via `GEMINI_MODEL` |
| `llm.ollama.base-url` | `http://localhost:11434` | Set via `OLLAMA_BASE_URL` (use `http://host.docker.internal:11434` from inside Docker if Ollama runs on the host) |
| `llm.ollama.model` | `llama3.1` | Set via `OLLAMA_MODEL` |

## Notes / extension points

- **Large files / async**: for very large batches, consider making `/api/extract`
  return a job ID and processing asynchronously (e.g. with Spring's `@Async` or a
  queue) rather than blocking the HTTP request.
- **Additional languages**: install extra `tesseract-ocr-<lang>` packages and set
  `ocr.language=eng+fra` (etc.).
- **Other formats** (PPTX, XLS, RTF, HTML): Apache POI/Tika already cover most of
  these — extend `FileTypeDetector` and add an extractor following the same pattern
  as `WordExtractor`.
