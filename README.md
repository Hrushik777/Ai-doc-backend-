# AI Document to Excel Backend

This Spring Boot backend receives a source document and an existing XLSX template. Its conversion pipeline is designed to understand the document, match extracted fields to the template headers, and return the populated workbook without rebuilding the template.

## Current architecture

- `controller` exposes HTTP endpoints only.
- `service.DocumentService` retains the original upload flow: validate, save into `Uploads/`, and persist metadata with JPA/MySQL.
- `service.validation` centralizes document and XLSX validation, including the 10 MB limit.
- `service.excel.ExcelService` reads headers from the first sheet and writes only mapped cells to the same workbook.
- `service.mapping` performs normalized exact header matching (trimmed, whitespace-collapsed, case-insensitive).
- `service.processing.DocumentProcessingService` orchestrates validation, extraction, mapping, and workbook generation.
- `service.understanding.DocumentUnderstandingService` is the provider-neutral extension point for OCR, Nemotron, a vision model, or an LLM.
- `globalexception` converts expected application failures into HTTP responses.

The default header row is zero-based row `0`. It can be changed through `app.excel.header-row-index` in `application.properties`. The default data row is the row immediately below the configured header row.

## Requirements and running

- Java 17 or later
- MySQL database named `aidoc` (configure its URL, username, and password in `src/main/resources/application.properties`)

Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
.\mvnw.cmd test
```

Tests use an in-memory H2 database and do not require MySQL.

## Endpoints

### Upload a source document

`POST /api/documents` remains unchanged. It accepts the multipart field named `file`, validates the existing allowed types, saves the file to `Uploads/`, and persists its metadata.

```powershell
curl.exe -X POST http://localhost:8080/api/documents `
  -F "file=@C:\files\scanned-document.pdf;type=application/pdf"
```

Allowed source types are PDF, DOCX, JPG/JPEG, and PNG. The maximum file size is 10 MB.

### Process a document into a template

`POST /api/documents/process` accepts two multipart fields:

- `document`: PDF, DOCX, JPG/JPEG, or PNG
- `template`: an `.xlsx` Excel template

```powershell
curl.exe -X POST http://localhost:8080/api/documents/process `
  -F "document=@C:\files\scanned-document.pdf;type=application/pdf" `
  -F "template=@C:\files\client-template.xlsx;type=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" `
  -o completed-document.xlsx
```

When a document-understanding provider is configured, the response is an XLSX attachment named `completed-document.xlsx`. Existing headers, other sheets, formatting, and unrelated cells are preserved as far as Apache POI permits; only cells in the first data row under exactly matched headers are changed.

For example, template headers `Pressure | Temperature | Material | Diameter` match extracted fields with the same normalized names. `" Pressure "` and `"pressure"` both match `Pressure`; no fuzzy matching is performed.

## Implemented now

- XLSX template validation, first-sheet header extraction, blank-header skipping, duplicate-header retention, and configurable header-row support.
- Header-to-field mapping and workbook population with Apache POI.
- Binary Excel download response wiring.
- Upload validation, persistence, and the original upload endpoint retained.
- Focused tests for Excel behavior, mapping normalization, validation, and both controller routes.

## Still to configure

No AI, OCR, Nemotron, vision model, or LLM integration has been invented. The supplied `NotConfiguredDocumentUnderstandingService` intentionally returns HTTP `501 Not Implemented` from `/api/documents/process` after document/template validation. Replace that one implementation with a real `DocumentUnderstandingService` to return `ExtractedDocumentData`; the rest of the pipeline is already wired to generate the completed workbook.
