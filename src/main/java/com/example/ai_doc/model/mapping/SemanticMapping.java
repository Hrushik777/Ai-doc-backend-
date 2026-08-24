package com.example.ai_doc.model.mapping;

/**
 * One structured mapping decision returned by the semantic reasoning model.
 */
public record SemanticMapping(
        String fieldId,
        String name,
        String value,
        int columnIndex,
        double confidence,
        String reason) {
}