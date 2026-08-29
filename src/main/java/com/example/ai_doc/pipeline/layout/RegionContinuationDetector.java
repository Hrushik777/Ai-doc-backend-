package com.example.ai_doc.pipeline.layout;

import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.ContinuationCandidate;
import com.example.ai_doc.domain.layout.LayoutCell;
import com.example.ai_doc.domain.layout.LayoutRegion;
import com.example.ai_doc.domain.layout.LayoutRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds content that may read <em>down and then across</em> rather than across each row -
 * a single list broken into two visual columns to save vertical space.
 *
 * <p>This is the component that answers the two-column question without ever hardcoding
 * "left" and "right". It deliberately does not decide. It produces the alternative merged
 * reading alongside the layout as it stands, records how much evidence supports the merge,
 * and leaves the choice to the mapping stage, which can weigh both readings against the
 * Excel template's own headers - the only place the information to decide actually exists.
 * A template of {@code Index | Value} wants the merged reading; a template of
 * {@code L | R} wants the columns kept apart.
 *
 * <p>Two shapes are recognised:
 * <ul>
 *   <li><b>Within one region</b> - the usual case. A grid whose left columns hold 1..6 and
 *       whose right columns pick up at 7 is one list, because a genuine pair of parallel
 *       readings would restart its numbering rather than continue it.</li>
 *   <li><b>Across regions</b> - when the columns' rows do not line up, the layout analyzer
 *       has already cut them into separate regions, and compatible side-by-side regions are
 *       candidates on shape alone.</li>
 * </ul>
 */
@Component
public class RegionContinuationDetector {

    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*(\\d+)");

    /** Siblings must share most of their vertical extent to count as side by side. */
    private static final double MIN_VERTICAL_OVERLAP = 0.5;

    private static final double CONFIDENCE_WITH_NUMERIC_RUN = 0.9;
    private static final double CONFIDENCE_SHAPE_ONLY = 0.5;

    public List<ContinuationCandidate> detect(List<LayoutRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }

        List<ContinuationCandidate> candidates = new ArrayList<>();

        for (int index = 0; index < regions.size(); index++) {
            ContinuationCandidate withinRegion = detectWithinRegion(regions.get(index), index);
            if (withinRegion != null) {
                candidates.add(withinRegion);
            }
        }

        for (List<Integer> group : sideBySideGroups(regions)) {
            candidates.add(buildAcrossRegions(regions, group));
        }

        return candidates;
    }

    // ---------------------------------------------------------------- within one region

    /**
     * Looks for a column boundary that splits the region into a left half and a right half
     * whose leading numbers form one continuous run. Every boundary is tried, so a grid of
     * {@code n | value | n | value} is recognised as well as a plain two-column list.
     */
    private ContinuationCandidate detectWithinRegion(LayoutRegion region, int regionIndex) {
        if (region.columnCount() < 2 || region.rowCount() < 2) {
            return null;
        }

        for (int boundary = 1; boundary < region.columnCount(); boundary++) {
            List<Integer> left = leadingNumbersInRange(region, 0, boundary);
            List<Integer> right = leadingNumbersInRange(region, boundary, region.columnCount());

            if (isContinuousRun(left, right, region.rowCount())) {
                return new ContinuationCandidate(
                        List.of(regionIndex),
                        mergeColumnHalves(region, boundary),
                        CONFIDENCE_WITH_NUMERIC_RUN,
                        "Leading numbers run 1.." + left.get(left.size() - 1)
                                + " down the left columns and continue at "
                                + right.get(0) + " down the right columns");
            }
        }

        return null;
    }

    /**
     * Nearly every row must carry a number, but not quite all of them: a two-column list
     * almost always sits under a header band, and that row has no index to contribute.
     * Demanding a number from every row would reject exactly the layouts this exists for.
     */
    private boolean isContinuousRun(List<Integer> left, List<Integer> right, int rowCount) {
        int minimumNumbered = Math.max(2, rowCount - 1);
        return left.size() >= minimumNumbered
                && right.size() >= minimumNumbered
                && ascends(left)
                && ascends(right)
                && right.get(0) == left.get(left.size() - 1) + 1;
    }

    /**
     * Stacks the right-hand columns underneath the left-hand ones, so the grid becomes the
     * single list it visually represents. Cells are rebased to start at column 0, because in
     * the merged reading a value from the right-hand half is the same kind of thing as one
     * from the left.
     */
    private LayoutRegion mergeColumnHalves(LayoutRegion region, int boundary) {
        List<LayoutRow> mergedRows = new ArrayList<>(region.rowCount() * 2);

        for (LayoutRow row : region.rows()) {
            mergedRows.add(new LayoutRow(mergedRows.size(), cellsInRange(row, 0, boundary)));
        }
        for (LayoutRow row : region.rows()) {
            mergedRows.add(new LayoutRow(mergedRows.size(),
                    cellsInRange(row, boundary, region.columnCount())));
        }

        int mergedColumnCount = Math.max(boundary, region.columnCount() - boundary);

        return new LayoutRegion(
                region.page(),
                region.kind(),
                region.bounds(),
                mergedRows,
                mergedColumnCount,
                region.readingOrder());
    }

    private List<LayoutCell> cellsInRange(LayoutRow row, int fromColumn, int toColumn) {
        List<LayoutCell> inRange = new ArrayList<>();
        for (LayoutCell cell : row.cells()) {
            if (cell.columnIndex() >= fromColumn && cell.columnIndex() < toColumn) {
                inRange.add(new LayoutCell(cell.columnIndex() - fromColumn, cell.text(), cell.bbox()));
            }
        }
        return inRange;
    }

    private List<Integer> leadingNumbersInRange(LayoutRegion region, int fromColumn, int toColumn) {
        List<Integer> numbers = new ArrayList<>(region.rowCount());
        for (LayoutRow row : region.rows()) {
            List<LayoutCell> cells = cellsInRange(row, fromColumn, toColumn);
            if (cells.isEmpty()) {
                continue;
            }
            Integer number = leadingNumber(cells.get(0).text());
            if (number != null) {
                numbers.add(number);
            }
        }
        return numbers;
    }

    // ------------------------------------------------------------------ across regions

    /**
     * Groups regions that sit beside one another on the same page with compatible shape.
     * Compatibility is deliberately strict - same kind, same column count - because merging
     * a table into a paragraph would corrupt both readings, not just the merged one.
     */
    private List<List<Integer>> sideBySideGroups(List<LayoutRegion> regions) {
        Map<Integer, List<Integer>> indexesByPage = new LinkedHashMap<>();
        for (int i = 0; i < regions.size(); i++) {
            indexesByPage
                    .computeIfAbsent(regions.get(i).page(), key -> new ArrayList<>())
                    .add(i);
        }

        List<List<Integer>> groups = new ArrayList<>();

        for (List<Integer> pageIndexes : indexesByPage.values()) {
            List<Integer> ordered = new ArrayList<>(pageIndexes);
            ordered.sort(Comparator.comparingDouble(index -> regions.get(index).bounds().xmin()));

            List<Integer> current = new ArrayList<>();
            for (Integer index : ordered) {
                if (current.isEmpty()) {
                    current.add(index);
                    continue;
                }

                LayoutRegion previous = regions.get(current.get(current.size() - 1));
                LayoutRegion candidate = regions.get(index);

                if (isSideBySide(previous, candidate) && isShapeCompatible(previous, candidate)) {
                    current.add(index);
                } else {
                    if (current.size() >= 2) {
                        groups.add(new ArrayList<>(current));
                    }
                    current = new ArrayList<>();
                    current.add(index);
                }
            }
            if (current.size() >= 2) {
                groups.add(current);
            }
        }

        return groups;
    }

    private boolean isSideBySide(LayoutRegion left, LayoutRegion right) {
        BBox leftBounds = left.bounds();
        BBox rightBounds = right.bounds();
        return rightBounds.xmin() >= leftBounds.xmax()
                && leftBounds.verticalOverlapRatio(rightBounds) >= MIN_VERTICAL_OVERLAP;
    }

    private boolean isShapeCompatible(LayoutRegion left, LayoutRegion right) {
        return left.kind() == right.kind() && left.columnCount() == right.columnCount();
    }

    private ContinuationCandidate buildAcrossRegions(List<LayoutRegion> regions, List<Integer> group) {
        List<LayoutRegion> members = new ArrayList<>(group.size());
        for (Integer index : group) {
            members.add(regions.get(index));
        }

        boolean numericRun = hasContinuingNumericRun(members);

        return new ContinuationCandidate(
                group,
                stack(members),
                numericRun ? CONFIDENCE_WITH_NUMERIC_RUN : CONFIDENCE_SHAPE_ONLY,
                numericRun
                        ? "Leading numbers continue across the column gutter"
                        : "Regions sit side by side with matching shape");
    }

    private boolean hasContinuingNumericRun(List<LayoutRegion> members) {
        List<List<Integer>> sequences = new ArrayList<>(members.size());
        for (LayoutRegion region : members) {
            List<Integer> numbers = leadingNumbersInRange(region, 0, region.columnCount());
            if (numbers.size() != region.rowCount() || numbers.isEmpty() || !ascends(numbers)) {
                return false;
            }
            sequences.add(numbers);
        }

        for (int i = 1; i < sequences.size(); i++) {
            List<Integer> previous = sequences.get(i - 1);
            if (sequences.get(i).get(0) != previous.get(previous.size() - 1) + 1) {
                return false;
            }
        }
        return true;
    }

    /** Concatenates the members' rows in reading order into one region. */
    private LayoutRegion stack(List<LayoutRegion> members) {
        List<LayoutRow> mergedRows = new ArrayList<>();
        BBox bounds = null;
        int columnCount = 0;

        for (LayoutRegion member : members) {
            bounds = bounds == null ? member.bounds() : bounds.union(member.bounds());
            columnCount = Math.max(columnCount, member.columnCount());
            for (LayoutRow row : member.rows()) {
                mergedRows.add(new LayoutRow(mergedRows.size(), row.cells()));
            }
        }

        LayoutRegion first = members.get(0);
        return new LayoutRegion(
                first.page(),
                first.kind(),
                bounds == null ? new BBox(0, 0, 0, 0) : bounds,
                mergedRows,
                columnCount,
                first.readingOrder());
    }

    // ------------------------------------------------------------------------ helpers

    private Integer leadingNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = LEADING_NUMBER.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            // A run of digits too long to be an index is not one.
            return null;
        }
    }

    private boolean ascends(List<Integer> numbers) {
        for (int i = 1; i < numbers.size(); i++) {
            if (numbers.get(i) <= numbers.get(i - 1)) {
                return false;
            }
        }
        return true;
    }
}
