package com.example.ai_doc.service.layout;

import com.example.ai_doc.model.layout.DocumentElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The horizontal half of the recursive cut: separates a page into stacked slabs wherever
 * a run of blank space is clearly larger than the document's own line spacing.
 *
 * <p>This is what makes column detection work on a real page. A heading that spans the
 * full width sits directly above two columns of text; cutting for columns first is
 * impossible, because the heading occupies every x position and hides the gutter. Cutting
 * the heading off into its own slab first leaves a body slab that splits cleanly.
 *
 * <p>The threshold is relative to the document's own spacing rather than absolute, so
 * evenly spaced text - where every gap equals the median - is never split, while a genuine
 * section break stands out however loose or tight the overall layout is.
 */
@Component
public class VerticalSlabSplitter {

    /** A break must be at least this many times the document's usual line gap. */
    private static final double GAP_FACTOR = 2.0;

    /** ...and at least a full text height, so hairline variation never splits. */
    private static final double MIN_GAP_HEIGHTS = 1.0;

    private final RowBander rowBander;

    public VerticalSlabSplitter(RowBander rowBander) {
        this.rowBander = rowBander;
    }

    public List<List<DocumentElement>> split(List<DocumentElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }

        List<List<DocumentElement>> bands = rowBander.band(elements);
        if (bands.size() < 3) {
            // Two bands give a single gap, which is its own median - there is nothing to
            // judge it against, so never cut.
            return List.of(List.copyOf(elements));
        }

        List<Double> gaps = gapsBetweenBands(bands);
        double medianGap = Geometry.median(positiveOnly(gaps));
        double threshold = Math.max(
                medianGap * GAP_FACTOR,
                Geometry.medianHeight(elements) * MIN_GAP_HEIGHTS);

        List<List<DocumentElement>> slabs = new ArrayList<>();
        List<DocumentElement> current = new ArrayList<>(bands.get(0));

        for (int i = 1; i < bands.size(); i++) {
            if (gaps.get(i - 1) > threshold) {
                slabs.add(current);
                current = new ArrayList<>();
            }
            current.addAll(bands.get(i));
        }
        slabs.add(current);

        return slabs;
    }

    private List<Double> gapsBetweenBands(List<List<DocumentElement>> bands) {
        List<Double> gaps = new ArrayList<>(bands.size() - 1);
        for (int i = 1; i < bands.size(); i++) {
            double previousBottom = Geometry.boundsOf(bands.get(i - 1)).ymax();
            double currentTop = Geometry.boundsOf(bands.get(i)).ymin();
            gaps.add(currentTop - previousBottom);
        }
        return gaps;
    }

    private List<Double> positiveOnly(List<Double> values) {
        List<Double> positive = new ArrayList<>(values.size());
        for (Double value : values) {
            if (value > 0) {
                positive.add(value);
            }
        }
        return positive;
    }
}
