package com.example.ai_doc.domain.result;

import java.util.List;

/** Binary response produced by the batch document-to-template conversion pipeline. */
public record BatchProcessedExcelFile(String filename, byte[] content, List<BatchItemResult> results) {
}
