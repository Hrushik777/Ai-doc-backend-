package com.example.ai_doc.model.layout;

import java.util.List;

/**
 * A visually coherent block of one page - a table, a key-value list, a bulleted list, or
 * a paragraph - with its rows already banded and its cells already assigned to columns.
 *
 * <p>{@code readingOrder} places the region in the sequence a person would read the
 * document in: page, then left-to-right across a column split, then top to bottom.
 */
public record LayoutRegion(
        int page,
        RegionKind kind,
        BBox bounds,
        List<LayoutRow> rows,
        int columnCount,
        int readingOrder) {

    public LayoutRegion {
        rows = List.copyOf(rows);
    }

    public LayoutRegion withReadingOrder(int newReadingOrder) {
        return new LayoutRegion(page, kind, bounds, rows, columnCount, newReadingOrder);
    }

    public LayoutRegion withKind(RegionKind newKind) {
        return new LayoutRegion(page, newKind, bounds, rows, columnCount, readingOrder);
    }

    public int rowCount() {
        return rows.size();
    }
}
