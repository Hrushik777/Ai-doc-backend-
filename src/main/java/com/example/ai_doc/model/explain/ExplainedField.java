package com.example.ai_doc.model.explain;

/**
 * One extracted document field, exposed for the mapping visualization.
 *
 * <p>Geometry is whatever the parse model reported and may be absent. Fields split
 * out of a table share their parent table's rectangle, so several fields can carry
 * identical coordinates.
 */
public record ExplainedField(
        int index,
        String name,
        String value,
        Integer pageNumber,
        Double x,
        Double y,
        Double width,
        Double height,
        String sourceType) {
}
