package com.example.ai_doc.model.mapping;

/** One structured mapping decision returned by the semantic reasoning model. */
public record SemanticMapping(
        int fieldIndex,
        int columnIndex,
        double confidence,
        String reason) {
}
