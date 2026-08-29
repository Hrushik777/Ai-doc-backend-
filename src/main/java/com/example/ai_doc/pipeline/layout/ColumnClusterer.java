package com.example.ai_doc.pipeline.layout;

import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.DocumentElement;
import com.example.ai_doc.domain.layout.LayoutCell;
import com.example.ai_doc.domain.layout.LayoutRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Turns banded rows into a grid by clustering the elements' left edges into column slots.
 *
 * <p>Clustering on the left edge rather than on the centre is what recovers a table whose
 * cells hold text of different lengths: the values are left-aligned, so their starts line
 * up even though their centres do not. A row that skips a column keeps its remaining cells
 * in the correct slots, which is what lets a sparse table still map positionally.
 */
@Component
public class ColumnClusterer {

    /** Two left edges within this many text heights belong to the same column. */
    private static final double TOLERANCE_HEIGHTS = 0.5;

    /** ...with a floor relative to region width, for very short text. */
    private static final double TOLERANCE_WIDTH_FRACTION = 0.015;

    /** Rows with their cells assigned to column slots, plus how many slots were found. */
    public record ColumnAssignment(List<LayoutRow> rows, int columnCount) {

        public ColumnAssignment {
            rows = List.copyOf(rows);
        }
    }

    public ColumnAssignment assign(List<List<DocumentElement>> bands) {
        if (bands == null || bands.isEmpty()) {
            return new ColumnAssignment(List.of(), 0);
        }

        List<DocumentElement> allElements = new ArrayList<>();
        bands.forEach(allElements::addAll);
        if (allElements.isEmpty()) {
            return new ColumnAssignment(List.of(), 0);
        }

        BBox bounds = Geometry.boundsOf(allElements);
        double tolerance = Math.max(
                Geometry.medianHeight(allElements) * TOLERANCE_HEIGHTS,
                bounds.width() * TOLERANCE_WIDTH_FRACTION);

        List<Double> columnStarts = clusterLeftEdges(allElements, tolerance);

        List<LayoutRow> rows = new ArrayList<>(bands.size());
        for (int rowIndex = 0; rowIndex < bands.size(); rowIndex++) {
            List<LayoutCell> cells = new ArrayList<>(bands.get(rowIndex).size());
            for (DocumentElement element : bands.get(rowIndex)) {
                cells.add(new LayoutCell(
                        nearestColumn(element.bbox().xmin(), columnStarts),
                        element.textOrEmpty(),
                        element.bbox()));
            }
            cells.sort(Comparator.comparingInt(LayoutCell::columnIndex));
            rows.add(new LayoutRow(rowIndex, cells));
        }

        return new ColumnAssignment(rows, columnStarts.size());
    }

    private List<Double> clusterLeftEdges(List<DocumentElement> elements, double tolerance) {
        List<Double> leftEdges = new ArrayList<>(elements.size());
        for (DocumentElement element : elements) {
            leftEdges.add(element.bbox().xmin());
        }
        Collections.sort(leftEdges);

        List<Double> centres = new ArrayList<>();
        double clusterSum = leftEdges.get(0);
        int clusterSize = 1;
        double clusterStart = leftEdges.get(0);

        for (int i = 1; i < leftEdges.size(); i++) {
            double edge = leftEdges.get(i);
            // Measured against the cluster's first edge, not its previous one: comparing
            // to the previous edge lets a run of slightly drifting values chain into one
            // cluster that spans several real columns.
            if (edge - clusterStart <= tolerance) {
                clusterSum += edge;
                clusterSize++;
            } else {
                centres.add(clusterSum / clusterSize);
                clusterSum = edge;
                clusterSize = 1;
                clusterStart = edge;
            }
        }
        centres.add(clusterSum / clusterSize);

        return centres;
    }

    private int nearestColumn(double leftEdge, List<Double> columnStarts) {
        int nearest = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < columnStarts.size(); i++) {
            double distance = Math.abs(columnStarts.get(i) - leftEdge);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }
}
