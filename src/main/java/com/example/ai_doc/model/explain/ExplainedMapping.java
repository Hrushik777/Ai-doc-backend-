package com.example.ai_doc.model.explain;

/**
 * One resolved decision to place an extracted value in a template column, on a given row.
 *
 * <p>{@code rowIndex} identifies which output row the value landed on: one document can now
 * fill many rows, so a mapping is only locatable with it.
 *
 * <p>Provenance comes in two shapes depending on the stage that resolved the value.
 * Structural mappings carry {@code page} and a rectangle, because they were read from a
 * position in the layout rather than from a named field. Deterministic and semantic
 * mappings instead carry {@code fieldIndex} into the extracted-field list, with
 * {@code fieldId} ("field-3", or "field-3-1" when one source field yields several values)
 * preserving the link for semantic ones, which are rebuilt without the document index.
 */
public record ExplainedMapping(
        int rowIndex,
        int fieldIndex,
        String fieldId,
        int columnIndex,
        String value,
        double confidence,
        String source,
        String reason,
        Integer pageNumber,
        Double x,
        Double y,
        Double width,
        Double height) {
}
