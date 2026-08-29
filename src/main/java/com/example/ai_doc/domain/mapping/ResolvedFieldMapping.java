package com.example.ai_doc.domain.mapping;

/** A validated decision to place one extracted value in one Excel column. */
public record ResolvedFieldMapping(
        int fieldIndex,
        int columnIndex,
        String value,
        double confidence,
        MappingSource source) {
}
