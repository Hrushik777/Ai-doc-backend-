package com.example.ai_doc.service.layout;

import com.example.ai_doc.model.layout.BBox;
import com.example.ai_doc.model.layout.DocumentElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the vertical whitespace gutters that separate a page into side-by-side regions -
 * the cut that turns
 *
 * <pre>
 *   1 - 3.5      7 - 4.57
 *   2 - 3.08     8 - 3.67
 * </pre>
 *
 * into two regions instead of one confused block of text.
 *
 * <p>A gutter is a band of x positions that <em>no</em> element occupies anywhere down the
 * region. Because occupancy is taken over every element at once, a run of empty columns is
 * by construction empty for the region's full height, so no separate "does it span far
 * enough vertically" test is needed. An element that crosses the gap - a title spanning
 * both columns - fills those columns and correctly suppresses the cut; splitting the title
 * off first is the horizontal cut's job, and {@link LayoutAnalyzer} alternates the two.
 */
@Component
public class ColumnGutterDetector {

    private static final int BINS = 1000;

    /**
     * A gutter must be wider than this many text heights. Inter-word spacing runs around a
     * third of a text height, so 1.5 clears ordinary spacing by a wide margin while staying
     * under the gap a real column split leaves.
     *
     * <p>Deliberately expressed against text height rather than against the median
     * inter-word gap: on a two-column page where every row holds exactly one element per
     * column, the only gaps that exist <em>are</em> the gutter, so a gap-relative threshold
     * would measure itself and never fire.
     */
    private static final double MIN_GUTTER_HEIGHTS = 1.5;

    /** And it must be wide in absolute terms too, so a narrow table gap cannot qualify. */
    private static final double MIN_GUTTER_WIDTH_FRACTION = 0.04;

    /**
     * Splits the elements on every qualifying gutter, left to right. Returns a single group
     * when the region has no column structure.
     */
    public List<List<DocumentElement>> split(List<DocumentElement> elements) {
        if (elements == null || elements.size() < 2) {
            return elements == null || elements.isEmpty() ? List.of() : List.of(List.copyOf(elements));
        }

        BBox bounds = Geometry.boundsOf(elements);
        double width = bounds.width();
        if (width <= 0) {
            return List.of(List.copyOf(elements));
        }

        boolean[] occupied = occupancyHistogram(elements, bounds, width);
        double minGutterWidth = Math.max(
                Geometry.medianHeight(elements) * MIN_GUTTER_HEIGHTS,
                width * MIN_GUTTER_WIDTH_FRACTION);

        List<Double> splitPositions = findGutterCentres(occupied, bounds, width, minGutterWidth);
        if (splitPositions.isEmpty()) {
            return List.of(List.copyOf(elements));
        }

        return partition(elements, splitPositions);
    }

    private boolean[] occupancyHistogram(List<DocumentElement> elements, BBox bounds, double width) {
        boolean[] occupied = new boolean[BINS];
        for (DocumentElement element : elements) {
            int startBin = binOf(element.bbox().xmin(), bounds, width);
            int endBin = binOf(element.bbox().xmax(), bounds, width);
            for (int bin = startBin; bin <= endBin; bin++) {
                occupied[bin] = true;
            }
        }
        return occupied;
    }

    private int binOf(double x, BBox bounds, double width) {
        int bin = (int) Math.floor((x - bounds.xmin()) / width * BINS);
        return Math.max(0, Math.min(BINS - 1, bin));
    }

    /**
     * Interior runs only. Empty bins at either edge are the region's own margin, not a
     * gutter between two things.
     */
    private List<Double> findGutterCentres(boolean[] occupied, BBox bounds, double width,
                                           double minGutterWidth) {
        List<Double> centres = new ArrayList<>();
        int runStart = -1;
        boolean seenOccupied = false;

        for (int bin = 0; bin < BINS; bin++) {
            if (occupied[bin]) {
                if (runStart >= 0 && seenOccupied) {
                    addIfWideEnough(centres, runStart, bin - 1, bounds, width, minGutterWidth);
                }
                runStart = -1;
                seenOccupied = true;
            } else if (runStart < 0) {
                runStart = bin;
            }
        }

        return centres;
    }

    private void addIfWideEnough(List<Double> centres, int runStart, int runEnd,
                                 BBox bounds, double width, double minGutterWidth) {
        double runWidth = (runEnd - runStart + 1) / (double) BINS * width;
        if (runWidth >= minGutterWidth) {
            double centreBin = (runStart + runEnd) / 2.0;
            centres.add(bounds.xmin() + centreBin / BINS * width);
        }
    }

    private List<List<DocumentElement>> partition(List<DocumentElement> elements,
                                                  List<Double> splitPositions) {
        List<List<DocumentElement>> groups = new ArrayList<>(splitPositions.size() + 1);
        for (int i = 0; i <= splitPositions.size(); i++) {
            groups.add(new ArrayList<>());
        }

        for (DocumentElement element : elements) {
            double centre = element.bbox().xCenter();
            int group = 0;
            while (group < splitPositions.size() && centre > splitPositions.get(group)) {
                group++;
            }
            groups.get(group).add(element);
        }

        List<List<DocumentElement>> nonEmpty = new ArrayList<>(groups.size());
        for (List<DocumentElement> group : groups) {
            if (!group.isEmpty()) {
                nonEmpty.add(group);
            }
        }
        return nonEmpty;
    }
}
