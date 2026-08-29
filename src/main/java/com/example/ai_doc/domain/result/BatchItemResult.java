package com.example.ai_doc.domain.result;

/**
 * Outcome of processing a single document within a batch request.
 *
 * <p>{@code rowIndex} is the first row the document's output landed on and
 * {@code rowsWritten} how many rows it occupies. Both are needed now that a document can
 * fill a gap in an existing row and append several more: one index no longer describes
 * where its output went.
 */
public record BatchItemResult(String filename,
                              boolean success,
                              Integer rowIndex,
                              Integer rowsWritten,
                              String errorMessage) {
}
