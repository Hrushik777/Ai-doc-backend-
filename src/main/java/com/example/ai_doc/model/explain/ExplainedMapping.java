package com.example.ai_doc.model.explain;

/**
 * One resolved decision to place an extracted value in a template column.
 *
 * <p>{@code fieldIndex} is -1 for semantic mappings, because the semantic stage
 * rebuilds them without the document index; {@code fieldId} ("field-3", or
 * "field-3-1" when one source field yields several values) preserves the link
 * back to the field the value came from.
 */
public record ExplainedMapping(
        int fieldIndex,
        String fieldId,
        int columnIndex,
        String value,
        double confidence,
        String source,
        String reason) {
}
