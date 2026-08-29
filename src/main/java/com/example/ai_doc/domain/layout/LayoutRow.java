package com.example.ai_doc.domain.layout;

import java.util.List;

/** One horizontal band of cells, ordered left to right. */
public record LayoutRow(int rowIndex, List<LayoutCell> cells) {

    public LayoutRow {
        cells = List.copyOf(cells);
    }

    /** Text of the cell in the given column slot, or null when the row does not fill it. */
    public String textAt(int columnIndex) {
        for (LayoutCell cell : cells) {
            if (cell.columnIndex() == columnIndex) {
                return cell.text();
            }
        }
        return null;
    }
}
