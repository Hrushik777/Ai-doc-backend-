package com.example.ai_doc.model.processing;

/** Binary response produced by the document-to-template conversion pipeline. */
public record ProcessedExcelFile(String filename, byte[] content) {
}
