package com.example.ai_doc.domain.layout;

/** One text element placed at a resolved (row, column) position within a region. */
public record LayoutCell(int columnIndex, String text, BBox bbox) {
}
