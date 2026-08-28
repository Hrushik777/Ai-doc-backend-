package com.example.ai_doc.model.explain;

/**
 * One extracted document field, exposed for the mapping visualization.
 *
 * <p>Geometry is whatever the parse model reported and may be absent. Cells split out of
 * a table now carry their own rectangle, interpolated across the parent table by row and
 * column, so they can be drawn individually - they used to share the parent's box, which
 * gave every cell in a table identical coordinates.
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
