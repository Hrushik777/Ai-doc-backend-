package com.example.ai_doc.service.layout;

import com.example.ai_doc.model.layout.LayoutCell;
import com.example.ai_doc.model.layout.LayoutRow;
import com.example.ai_doc.model.layout.RegionKind;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Decides what a region <em>is</em> - a table, labelled key-value pairs, a list, or prose -
 * from its own geometry and text shape, with no model call.
 *
 * <p>The order of the rules matters. A block of five labelled pairs is geometrically
 * indistinguishable from a five-row two-column table: both are three-or-more rows in two
 * aligned columns. What separates them is the label punctuation, so the key-value rule is
 * tested first and the table rule only sees what it rejects. Getting this backwards makes
 * every equipment sheet look like a table with a missing header row.
 */
@Component
public class RegionClassifier {

    private static final Pattern LEADING_BULLET = Pattern.compile("^\\s*[-*•·]\\s+");
    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*\\d+\\s*[.):\\-]");
    private static final Pattern LEADING_LETTER = Pattern.compile("^\\s*[a-zA-Z]\\s*[.)]\\s+");

    /** Fraction of rows that must show a trait before it decides the region. */
    private static final double MAJORITY = 0.6;

    /** Fraction of rows a table must have at its modal width before it counts as aligned. */
    private static final double TABLE_ALIGNMENT = 0.7;

    private static final int MIN_TABLE_ROWS = 3;
    private static final int MIN_LIST_ROWS = 3;

    /** A label is short; a paragraph that happens to contain a colon is not. */
    private static final int MAX_LABEL_LENGTH = 60;

    public RegionKind classify(List<LayoutRow> rows, int columnCount) {
        if (rows == null || rows.isEmpty()) {
            return RegionKind.PROSE;
        }

        if (fractionMatching(rows, this::looksLikeKeyValue) >= MAJORITY) {
            return RegionKind.KEY_VALUE;
        }

        if (rows.size() >= MIN_TABLE_ROWS && columnCount >= 2 && isAligned(rows)) {
            return RegionKind.TABLE;
        }

        if (rows.size() >= MIN_LIST_ROWS && fractionMatching(rows, this::looksLikeListItem) >= MAJORITY) {
            return RegionKind.LIST;
        }

        return RegionKind.PROSE;
    }

    /**
     * A row is key-value shaped when its leading cell carries label punctuation - either
     * "Label: value" inside one cell, or a "Label:" cell beside its value.
     */
    private boolean looksLikeKeyValue(LayoutRow row) {
        String first = firstCellText(row);
        if (first.isBlank()) {
            return false;
        }

        int colon = first.indexOf(':');
        if (colon > 0 && colon <= MAX_LABEL_LENGTH) {
            // "Label: value" in one cell, or a bare "Label:" cell.
            return colon == first.length() - 1 || row.cells().size() <= 2;
        }

        return false;
    }

    private boolean looksLikeListItem(LayoutRow row) {
        String first = firstCellText(row);
        return LEADING_BULLET.matcher(first).find()
                || LEADING_NUMBER.matcher(first).find()
                || LEADING_LETTER.matcher(first).find();
    }

    /**
     * Most rows have the same number of cells. A real table is regular; a stack of prose
     * lines that happened to cluster into columns is not.
     */
    private boolean isAligned(List<LayoutRow> rows) {
        Map<Integer, Integer> countsByWidth = new HashMap<>();
        for (LayoutRow row : rows) {
            countsByWidth.merge(row.cells().size(), 1, Integer::sum);
        }

        int modalWidth = 0;
        int modalCount = 0;
        for (Map.Entry<Integer, Integer> entry : countsByWidth.entrySet()) {
            if (entry.getValue() > modalCount) {
                modalCount = entry.getValue();
                modalWidth = entry.getKey();
            }
        }

        return modalWidth >= 2 && (double) modalCount / rows.size() >= TABLE_ALIGNMENT;
    }

    private double fractionMatching(List<LayoutRow> rows, java.util.function.Predicate<LayoutRow> trait) {
        int matches = 0;
        for (LayoutRow row : rows) {
            if (trait.test(row)) {
                matches++;
            }
        }
        return (double) matches / rows.size();
    }

    private String firstCellText(LayoutRow row) {
        List<LayoutCell> cells = row.cells();
        if (cells.isEmpty()) {
            return "";
        }
        String text = cells.get(0).text();
        return text == null ? "" : text;
    }
}
