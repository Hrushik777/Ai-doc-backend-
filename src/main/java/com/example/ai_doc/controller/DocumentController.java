package com.example.ai_doc.controller;

import com.example.ai_doc.service.DocumentService;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.service.processing.DocumentProcessingService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
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

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> processDocument(
            @RequestParam("document") MultipartFile document,
            @RequestParam("template") MultipartFile template) {

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
}
