package com.example.ai_doc.pipeline.layout;

import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.DocumentElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small shared statistics used by every stage of the layout analysis. */
final class Geometry {

    private Geometry() {
    }

    static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(middle)
                : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    /**
     * Typical text height for a set of elements. Every tolerance in the analysis is
     * expressed as a multiple of this rather than as an absolute distance, so the same
     * thresholds hold for a dense table and for large handwriting.
     */
    static double medianHeight(List<DocumentElement> elements) {
        List<Double> heights = new ArrayList<>(elements.size());
        for (DocumentElement element : elements) {
            double height = element.bbox().height();
            if (height > 0) {
                heights.add(height);
            }
        }
        return median(heights);
    }

    /** Half of the shorter box must be shared before overlap alone puts two boxes on a line. */
    static final double SAME_LINE_OVERLAP = 0.5;

    /**
     * Centre-distance fallback, as a multiple of the median text height. Two words on one
     * handwritten line can sit around 0.6 heights apart with little overlap between their
     * boxes; consecutive lines are further apart than this even when spacing is uneven.
     */
    static final double SAME_LINE_CENTER_FACTOR = 0.7;

    /**
     * Whether two boxes read as being on the same line.
     *
     * <p>Shared so that every stage agrees on what a row is. When the row bander and the
     * cut logic use different rules, a page of drifting handwritten text bands correctly
     * into rows and is then cut apart column by column because the second rule disagrees
     * that those rows line up.
     */
    static boolean onSameLine(BBox first, BBox second, double medianHeight) {
        return first.verticalOverlapRatio(second) > SAME_LINE_OVERLAP
                || Math.abs(first.yCenter() - second.yCenter()) < medianHeight * SAME_LINE_CENTER_FACTOR;
    }

    static BBox boundsOf(List<DocumentElement> elements) {
        BBox bounds = null;
        for (DocumentElement element : elements) {
            bounds = bounds == null ? element.bbox() : bounds.union(element.bbox());
        }
        return bounds == null ? new BBox(0, 0, 0, 0) : bounds;
    }
}
