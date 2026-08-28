package com.example.ai_doc.model.processing;

/** Outcome of processing a single document within a batch request. */
public record BatchItemResult(String filename, boolean success, Integer rowIndex, String errorMessage) {
}
