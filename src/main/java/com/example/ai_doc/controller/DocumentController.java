package com.example.ai_doc.controller;

import com.example.ai_doc.service.DocumentService;
import com.example.ai_doc.model.processing.BatchItemResult;
import com.example.ai_doc.model.processing.BatchProcessedExcelFile;
import com.example.ai_doc.model.explain.ProcessExplanation;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.service.processing.DocumentProcessingService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(
        origins = {"http://localhost:5173", "http://localhost:5174"},
        // Without these the browser cannot read the batch outcome headers below:
        // they are not CORS-safelisted, so fetch() silently hides them.
        exposedHeaders = {"X-Batch-Total-Count", "X-Batch-Success-Count", "X-Batch-Failed-Files"})
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;

    public DocumentController(DocumentService documentService,
                              DocumentProcessingService documentProcessingService) {
        this.documentService = documentService;
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        documentService.saveDocument(file);

        return ResponseEntity.ok("Document uploaded successfully");
    }

    /**
     * Fills the supplied template from the document. The template is optional: without one,
     * the columns are inferred from the document itself and a workbook is generated around
     * them.
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> processDocument(
            @RequestParam("document") MultipartFile document,
            @RequestParam(value = "template", required = false) MultipartFile template) {

        ProcessedExcelFile completedFile = documentProcessingService.process(document, template);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(completedFile.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(completedFile.content());
    }

    /**
     * Same pipeline as {@code /process}, but returns JSON: the workbook plus the extracted
     * fields and resolved mappings behind it, so a client can show how the document was read.
     * The template is optional here too.
     */
    @PostMapping(value = "/process/explain", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessExplanation> explainDocument(
            @RequestParam("document") MultipartFile document,
            @RequestParam(value = "template", required = false) MultipartFile template) {

        return ResponseEntity.ok(documentProcessingService.explain(document, template));
    }

    @PostMapping(value = "/process/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> processBatchDocuments(
            @RequestParam("documents") List<MultipartFile> documents,
            @RequestParam(value = "template", required = false) MultipartFile template) {

        BatchProcessedExcelFile result = documentProcessingService.processBatch(documents, template);

        long successCount = result.results().stream().filter(BatchItemResult::success).count();
        String failedFilenames = result.results().stream()
                .filter(item -> !item.success())
                .map(BatchItemResult::filename)
                .collect(Collectors.joining(","));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(result.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header("X-Batch-Total-Count", String.valueOf(result.results().size()))
                .header("X-Batch-Success-Count", String.valueOf(successCount))
                .header("X-Batch-Failed-Files", failedFilenames)
                .body(result.content());
    }
}
