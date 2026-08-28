package com.example.ai_doc.model.layout;

import java.util.List;

/**
 * Two or more side-by-side regions that may be one list broken across visual columns
 * rather than separate fields.
 *
 * <p>Geometry cannot settle this on its own. A page showing
 *
 * <pre>
 *   L                 R
 *   1 - 3.5           7 - 4.57
 *   2 - 3.08          8 - 3.67
 * </pre>
 *
 * is equally consistent with "twelve measurements continued in a second column" and with
 * "a left-side reading beside a right-side reading". So this record carries <em>both</em>
 * readings - {@link #mergedReading()} and the untouched sibling regions - and the mapping
 * stage picks whichever the Excel template's own headers support. That is what keeps the
 * L/R structure out of the geometry code.
 */
public record ContinuationCandidate(
        List<Integer> regionIndexes,
        LayoutRegion mergedReading,
        double confidence,
        String evidence) {

    public ContinuationCandidate {
        regionIndexes = List.copyOf(regionIndexes);
    }
}
