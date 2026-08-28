package com.example.ai_doc.service.understanding;

import com.example.ai_doc.model.layout.BBox;
import com.example.ai_doc.model.layout.DocumentElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a table element into one element per cell, each with its own rectangle.
 *
 * <p>The rectangles matter more than the split. Cells used to inherit the parent table's
 * box verbatim, so every cell in a table reported identical coordinates - which made the
 * table the one part of a document that spatial reasoning could say nothing about. The
 * positions here are interpolated across the parent box by row and column, so they are
 * approximate, but approximate positions in the right order support row banding and column
 * clustering, and identical ones support nothing at all.
 */
final class TableCellSplitter {

    /** Nemotron returns table content in LaTeX-like form; rows end with a double backslash. */
    private static final String ROW_SEPARATOR = "\\\\\\\\|\\r?\\n";

    private static final String CELL_SEPARATOR = "&";

    private TableCellSplitter() {
    }

    /**
     * Returns one element per parsed cell, or the original element unchanged when the
     * content does not parse as a grid.
     */
    static List<DocumentElement> split(DocumentElement table) {
        String text = table.textOrEmpty();
        if (text.isBlank()) {
            return List.of(table);
        }

        List<List<String>> grid = parseGrid(text);
        if (grid.isEmpty()) {
            return List.of(table);
        }

        int columnCount = 0;
        for (List<String> row : grid) {
            columnCount = Math.max(columnCount, row.size());
        }
        if (columnCount == 0) {
            return List.of(table);
        }

        return toElements(table, grid, columnCount);
    }

    private static List<List<String>> parseGrid(String text) {
        List<List<String>> grid = new ArrayList<>();

        for (String rawRow : text.split(ROW_SEPARATOR)) {
            String row = stripLatexWrappers(rawRow);
            if (row.isBlank()) {
                continue;
            }

            List<String> cells = new ArrayList<>();
            for (String rawCell : row.split(CELL_SEPARATOR, -1)) {
                cells.add(cleanCellText(rawCell));
            }

            // A row that never had a separator is a caption or a stray line, not a table row.
            if (cells.size() >= 2 && cells.stream().anyMatch(cell -> !cell.isBlank())) {
                grid.add(cells);
            }
        }

        return grid;
    }

    private static List<DocumentElement> toElements(DocumentElement table,
                                                    List<List<String>> grid,
                                                    int columnCount) {
        BBox bounds = table.bbox();
        double rowHeight = bounds.height() / grid.size();
        double columnWidth = bounds.width() / columnCount;

        List<DocumentElement> elements = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < grid.size(); rowIndex++) {
            List<String> row = grid.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String cellText = row.get(columnIndex);
                if (cellText.isBlank()) {
                    continue;
                }

                double xmin = bounds.xmin() + columnIndex * columnWidth;
                double ymin = bounds.ymin() + rowIndex * rowHeight;

                elements.add(new DocumentElement(
                        table.page(),
                        cellText,
                        "table-cell",
                        new BBox(xmin, ymin, xmin + columnWidth, ymin + rowHeight),
                        table.confidence()));
            }
        }

        return elements.isEmpty() ? List.of(table) : elements;
    }

    private static String stripLatexWrappers(String row) {
        return row
                .replace("\\multicolumn{2}{c}{", "")
                .replace("\\begin{tabular}{cc}", "")
                .replace("\\end{tabular}", "")
                .strip();
    }

    private static String cleanCellText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\textbf{", "")
                .replace("}", "")
                .replace("**", "")
                .strip();
    }
}
