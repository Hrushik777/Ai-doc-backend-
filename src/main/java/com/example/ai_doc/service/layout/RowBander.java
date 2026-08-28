package com.example.ai_doc.service.layout;

import com.example.ai_doc.model.layout.DocumentElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Groups elements into the horizontal bands a reader would see as single lines.
 *
 * <p>Banding is decided by <em>vertical overlap</em> rather than by equality of y, which
 * is what lets it survive the two layouts that break a fixed-threshold approach:
 * irregular line spacing, and the baseline drift in handwriting where no two words on a
 * line share a y coordinate. An element joins the current band when it overlaps that
 * band's vertical span, or - for a badly drifting line where the overlap alone is not
 * enough - when its centre is still within a fraction of a text height of the band's.
 */
@Component
public class RowBander {

    /**
     * Bands the given elements, top to bottom, each band ordered left to right.
     * The input list is not modified.
     */
    public List<List<DocumentElement>> band(List<DocumentElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }

        double medianHeight = Geometry.medianHeight(elements);
        double centerTolerance = medianHeight * Geometry.SAME_LINE_CENTER_FACTOR;

        List<DocumentElement> sorted = new ArrayList<>(elements);
        sorted.sort(Comparator.comparingDouble(element -> element.bbox().yCenter()));

        List<List<DocumentElement>> bands = new ArrayList<>();
        List<DocumentElement> current = new ArrayList<>();
        // The running mean of the band's member centres, not its outer extent: comparing
        // against the extent lets one tall element stretch a band until it swallows the
        // lines below it.
        double bandCenterSum = 0;

        for (DocumentElement element : sorted) {
            if (current.isEmpty()) {
                current.add(element);
                bandCenterSum = element.bbox().yCenter();
                continue;
            }

            double bandCenter = bandCenterSum / current.size();
            boolean overlaps = false;
            for (DocumentElement member : current) {
                if (member.bbox().verticalOverlapRatio(element.bbox()) > Geometry.SAME_LINE_OVERLAP) {
                    overlaps = true;
                    break;
                }
            }
            boolean centreIsClose =
                    Math.abs(element.bbox().yCenter() - bandCenter) < centerTolerance;

            if (overlaps || centreIsClose) {
                current.add(element);
                bandCenterSum += element.bbox().yCenter();
            } else {
                bands.add(orderedByX(current));
                current = new ArrayList<>();
                current.add(element);
                bandCenterSum = element.bbox().yCenter();
            }
        }

        if (!current.isEmpty()) {
            bands.add(orderedByX(current));
        }

        return bands;
    }

    private List<DocumentElement> orderedByX(List<DocumentElement> band) {
        List<DocumentElement> ordered = new ArrayList<>(band);
        ordered.sort(Comparator.comparingDouble(element -> element.bbox().xmin()));
        return ordered;
    }
}
