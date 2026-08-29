package com.example.ai_doc.pipeline.layout;

import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.ContinuationCandidate;
import com.example.ai_doc.domain.layout.DocumentElement;
import com.example.ai_doc.domain.layout.DocumentLayout;
import com.example.ai_doc.domain.layout.LayoutRegion;
import com.example.ai_doc.domain.layout.PageGeometry;
import com.example.ai_doc.domain.layout.ParsedDocument;
import com.example.ai_doc.domain.layout.RegionKind;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a flat list of parsed text elements into a structural reading of the document,
 * using coordinates alone. No model is called from anywhere in this package.
 *
 * <p>The segmentation is a recursive XY cut: cut horizontally on blank bands, then
 * vertically on gutters, then repeat. Alternating the two is what handles a real page - a
 * full-width heading above two columns hides the gutter until the heading has been cut
 * away into its own slab.
 *
 * <p>Regions come out already in reading order, because the two cuts emit their pieces in
 * order: slabs top to bottom, gutter parts left to right. Recursing over them in order
 * therefore yields the sequence a person would read, with no sorting heuristic needed
 * afterwards.
 */
@Component
public class LayoutAnalyzer {

    /**
     * Cuts alternate, so three levels covers a heading over columns over a nested table.
     * Beyond that the cuts are subdividing noise rather than finding structure.
     */
    private static final int MAX_CUT_DEPTH = 3;

    private final RowBander rowBander;
    private final VerticalSlabSplitter verticalSlabSplitter;
    private final ColumnGutterDetector columnGutterDetector;
    private final ColumnClusterer columnClusterer;
    private final RegionClassifier regionClassifier;
    private final RegionContinuationDetector regionContinuationDetector;

    public LayoutAnalyzer(RowBander rowBander,
                          VerticalSlabSplitter verticalSlabSplitter,
                          ColumnGutterDetector columnGutterDetector,
                          ColumnClusterer columnClusterer,
                          RegionClassifier regionClassifier,
                          RegionContinuationDetector regionContinuationDetector) {
        this.rowBander = rowBander;
        this.verticalSlabSplitter = verticalSlabSplitter;
        this.columnGutterDetector = columnGutterDetector;
        this.columnClusterer = columnClusterer;
        this.regionClassifier = regionClassifier;
        this.regionContinuationDetector = regionContinuationDetector;
    }

    /**
     * @param elements every parsed element, in any order, in raw page coordinates
     * @param pages    the size of each page, used to scale coordinates into [0,1]
     */
    public DocumentLayout analyze(List<DocumentElement> elements, List<PageGeometry> pages) {
        if (elements == null || elements.isEmpty()) {
            return DocumentLayout.empty();
        }

        List<DocumentElement> normalized = normalize(elements, pages);
        List<LayoutRegion> regions = new ArrayList<>();

        for (Map.Entry<Integer, List<DocumentElement>> page : groupByPage(normalized).entrySet()) {
            for (List<DocumentElement> group : segment(page.getValue(), 0)) {
                regions.add(toRegion(page.getKey(), group, regions.size()));
            }
        }

        List<ContinuationCandidate> continuations = regionContinuationDetector.detect(regions);
        return new DocumentLayout(pages == null ? List.of() : pages, regions, continuations);
    }

    /**
     * Scales every element into [0,1] against its own page, so a threshold expressed as a
     * fraction of the page means the same thing on A4 and on a phone photo, and so a
     * comparison between page 1 and page 3 is meaningful.
     */
    /**
     * Scales a parsed document into [0,1] without analysing it.
     *
     * <p>Exposed so that paths which bypass layout analysis - the raw field dump - still
     * report coordinates in the same space as everything else. Two endpoints reporting the
     * same rectangle in different units is the kind of inconsistency that is only ever
     * discovered by someone drawing a box in the wrong place.
     */
    public ParsedDocument normalized(ParsedDocument parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return ParsedDocument.empty();
        }
        return new ParsedDocument(normalize(parsed.elements(), parsed.pages()), parsed.pages());
    }

    private List<DocumentElement> normalize(List<DocumentElement> elements, List<PageGeometry> pages) {
        Map<Integer, PageGeometry> geometryByPage = new LinkedHashMap<>();
        if (pages != null) {
            for (PageGeometry geometry : pages) {
                geometryByPage.put(geometry.page(), geometry);
            }
        }

        List<DocumentElement> normalized = new ArrayList<>(elements.size());
        for (DocumentElement element : elements) {
            PageGeometry geometry = geometryByPage.get(element.page());
            if (geometry == null) {
                // Coordinates that are already normalized, or a page whose size was never
                // reported: leave them alone rather than inventing a scale.
                normalized.add(element);
                continue;
            }
            normalized.add(new DocumentElement(
                    element.page(),
                    element.text(),
                    element.type(),
                    element.bbox().normalizedBy(geometry.width(), geometry.height()),
                    element.confidence()));
        }
        return normalized;
    }

    private Map<Integer, List<DocumentElement>> groupByPage(List<DocumentElement> elements) {
        Map<Integer, List<DocumentElement>> byPage = new java.util.TreeMap<>();
        for (DocumentElement element : elements) {
            byPage.computeIfAbsent(element.page(), key -> new ArrayList<>()).add(element);
        }
        return byPage;
    }

    /** Recursive XY cut. Returns leaf groups in reading order. */
    private List<List<DocumentElement>> segment(List<DocumentElement> elements, int depth) {
        if (elements.size() < 2 || depth >= MAX_CUT_DEPTH) {
            return List.of(elements);
        }

        List<List<DocumentElement>> slabs = verticalSlabSplitter.split(elements);
        if (slabs.size() > 1) {
            return recurseInto(slabs, depth);
        }

        List<List<DocumentElement>> columns = columnGutterDetector.split(elements);
        if (columns.size() > 1 && !rowsAlignAcross(columns)) {
            return recurseInto(columns, depth);
        }

        return List.of(elements);
    }

    private List<List<DocumentElement>> recurseInto(List<List<DocumentElement>> parts, int depth) {
        List<List<DocumentElement>> result = new ArrayList<>();
        for (List<DocumentElement> part : parts) {
            result.addAll(segment(part, depth + 1));
        }
        return result;
    }

    /**
     * True when every part has the same number of row bands and corresponding bands line up
     * vertically - which means the gutter is the gap <em>between two columns of one table</em>,
     * not the gap between two independent blocks of page content.
     *
     * <p>Without this test the cut is actively harmful: the space between a column of labels
     * and its column of values is a perfectly good gutter, so every labelled equipment sheet
     * would be cut into a region of orphaned labels beside a region of orphaned values. The
     * same guard is what keeps a two-column measurement list as one grid, leaving the
     * question of whether it reads across or down to {@link RegionContinuationDetector}.
     */
    private boolean rowsAlignAcross(List<List<DocumentElement>> parts) {
        List<DocumentElement> allElements = new ArrayList<>();
        parts.forEach(allElements::addAll);
        double medianHeight = Geometry.medianHeight(allElements);

        List<List<List<DocumentElement>>> bandsPerPart = new ArrayList<>(parts.size());
        for (List<DocumentElement> part : parts) {
            bandsPerPart.add(rowBander.band(part));
        }

        int bandCount = bandsPerPart.get(0).size();
        if (bandCount == 0) {
            return false;
        }
        for (List<List<DocumentElement>> bands : bandsPerPart) {
            if (bands.size() != bandCount) {
                return false;
            }
        }

        for (int bandIndex = 0; bandIndex < bandCount; bandIndex++) {
            BBox reference = Geometry.boundsOf(bandsPerPart.get(0).get(bandIndex));
            for (int partIndex = 1; partIndex < bandsPerPart.size(); partIndex++) {
                BBox other = Geometry.boundsOf(bandsPerPart.get(partIndex).get(bandIndex));
                if (!Geometry.onSameLine(reference, other, medianHeight)) {
                    return false;
                }
            }
        }

        return true;
    }

    private LayoutRegion toRegion(int page, List<DocumentElement> elements, int readingOrder) {
        List<List<DocumentElement>> bands = rowBander.band(elements);
        ColumnClusterer.ColumnAssignment assignment = columnClusterer.assign(bands);
        RegionKind kind = regionClassifier.classify(assignment.rows(), assignment.columnCount());

        return new LayoutRegion(
                page,
                kind,
                Geometry.boundsOf(elements),
                assignment.rows(),
                assignment.columnCount(),
                readingOrder);
    }
}
