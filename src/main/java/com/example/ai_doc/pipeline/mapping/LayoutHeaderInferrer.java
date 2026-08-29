package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.layout.DocumentLayout;
import com.example.ai_doc.domain.layout.LayoutCell;
import com.example.ai_doc.domain.layout.LayoutRegion;
import com.example.ai_doc.domain.layout.LayoutRow;
import com.example.ai_doc.domain.layout.RegionKind;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads a document's own headers out of its layout, for the case where no template was
 * supplied and the columns have to be decided rather than given.
 *
 * <p>Deterministic and free. Most documents that need this already state their headers: a
 * table's first band names its columns, and a block of labelled pairs names its fields.
 * Asking a model to invent headers a document has already written down would be a call
 * spent re-deriving what is sitting in the geometry, so the model is only consulted when
 * this finds nothing - which it signals by returning an empty list.
 */
@Component
public class LayoutHeaderInferrer {

    /** Below this a "header band" is one stray cell, not a description of the columns. */
    private static final int MIN_TABLE_HEADERS = 2;

    /** A header names a column; anything this long is a sentence that landed in row one. */
    private static final int MAX_HEADER_LENGTH = 60;

    /**
     * Returns the headers the document states about itself, or an empty list when it states
     * none clearly enough to use.
     */
    public List<String> infer(DocumentLayout layout) {
        if (layout == null || layout.isEmpty()) {
            return List.of();
        }

        List<String> fromTable = headersFromLargestTable(layout);
        if (!fromTable.isEmpty()) {
            return fromTable;
        }

        return headersFromKeyValueLabels(layout);
    }

    /**
     * The widest table's header band. The widest is preferred over the first because a
     * document often opens with a small summary grid before the table that actually carries
     * its data.
     */
    private List<String> headersFromLargestTable(DocumentLayout layout) {
        LayoutRegion best = null;
        for (LayoutRegion region : layout.regions()) {
            if (region.kind() != RegionKind.TABLE && region.kind() != RegionKind.LIST) {
                continue;
            }
            if (region.rowCount() < 2) {
                continue;
            }
            if (best == null
                    || region.columnCount() > best.columnCount()
                    || (region.columnCount() == best.columnCount() && region.rowCount() > best.rowCount())) {
                best = region;
            }
        }

        if (best == null) {
            return List.of();
        }

        List<String> headers = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (LayoutCell cell : best.rows().get(0).cells()) {
            String header = cleanHeader(cell.text());
            // Duplicated names in a header band mean it is data, not headers.
            if (header.isEmpty() || !seen.add(header.toLowerCase(java.util.Locale.ROOT))) {
                return List.of();
            }
            headers.add(header);
        }

        return headers.size() >= MIN_TABLE_HEADERS ? headers : List.of();
    }

    /** Labelled pairs name their own fields: every label becomes a column. */
    private List<String> headersFromKeyValueLabels(DocumentLayout layout) {
        List<String> headers = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (LayoutRegion region : layout.regions()) {
            if (region.kind() != RegionKind.KEY_VALUE) {
                continue;
            }
            for (LayoutRow row : region.rows()) {
                String label = labelOf(row);
                if (label.isEmpty() || !seen.add(label.toLowerCase(java.util.Locale.ROOT))) {
                    continue;
                }
                headers.add(label);
            }
        }

        return headers;
    }

    private String labelOf(LayoutRow row) {
        if (row.cells().isEmpty()) {
            return "";
        }
        String first = row.cells().get(0).text();
        if (first == null) {
            return "";
        }
        int colon = first.indexOf(':');
        return cleanHeader(colon > 0 ? first.substring(0, colon) : first);
    }

    private String cleanHeader(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.strip();
        if (cleaned.endsWith(":")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).strip();
        }
        return cleaned.length() > MAX_HEADER_LENGTH ? "" : cleaned;
    }
}
