package com.example.ai_doc.pipeline.layout;

import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.ContinuationCandidate;
import com.example.ai_doc.domain.layout.DocumentElement;
import com.example.ai_doc.domain.layout.DocumentLayout;
import com.example.ai_doc.domain.layout.LayoutRegion;
import com.example.ai_doc.domain.layout.LayoutRow;
import com.example.ai_doc.domain.layout.PageGeometry;
import com.example.ai_doc.domain.layout.RegionKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every fixture here is hand-built geometry: no PDF, no parse model, no network. The point
 * of moving structure detection into Java is that the hard layouts become testable at all,
 * so these are the cases that used to be verifiable only by uploading a document and
 * looking at the spreadsheet.
 */
class LayoutAnalyzerTest {

    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*(\\d+)");

    private final RowBander rowBander = new RowBander();
    private final LayoutAnalyzer analyzer = new LayoutAnalyzer(
            rowBander,
            new VerticalSlabSplitter(rowBander),
            new ColumnGutterDetector(),
            new ColumnClusterer(),
            new RegionClassifier(),
            new RegionContinuationDetector());

    private static final PageGeometry PAGE_ONE = new PageGeometry(1, 1000, 1000);

    // ------------------------------------------------------------------ two-column list

    /**
     * The layout from the brief:
     *
     * <pre>
     *   L                 R
     *   1 - 3.5           7 - 4.57
     *   2 - 3.08          8 - 3.67
     * </pre>
     *
     * Both readings have to survive. The grid stays intact so a template of {@code L | R}
     * can pair them across, and a merged reading is offered so a template of
     * {@code Index | Value} can read them as one run of twelve.
     */
    @Test
    void twoColumnMeasurementListKeepsBothReadings() {
        DocumentLayout layout = analyzer.analyze(twoColumnMeasurementList(), List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(1);
        LayoutRegion region = layout.regions().get(0);
        assertThat(region.kind()).isEqualTo(RegionKind.TABLE);
        assertThat(region.columnCount()).isEqualTo(2);
        assertThat(region.rowCount()).isEqualTo(7);

        // The across reading: row 1 pairs the first left value with the first right value.
        assertThat(region.rows().get(1).textAt(0)).isEqualTo("1 - 3.5");
        assertThat(region.rows().get(1).textAt(1)).isEqualTo("7 - 4.57");

        // The down-then-across reading, offered but not chosen.
        assertThat(layout.continuations()).hasSize(1);
        ContinuationCandidate candidate = layout.continuations().get(0);
        assertThat(candidate.confidence()).isEqualTo(0.9);
        assertThat(candidate.mergedReading().rowCount()).isEqualTo(14);
        assertThat(leadingNumbersOf(candidate.mergedReading()))
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    /**
     * Two columns that both restart at 1 are parallel readings, not one continued list,
     * so no merged reading may be offered - otherwise a Left/Right measurement sheet would
     * be silently stacked into a single column of twelve.
     */
    @Test
    void parallelColumnsThatRestartTheirNumberingAreNotAContinuation() {
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(element("L", 100, 100, 120, 118));
        elements.add(element("R", 500, 100, 520, 118));
        for (int i = 0; i < 6; i++) {
            double y = 150 + i * 40;
            elements.add(element((i + 1) + " - 3.5", 100, y, 220, y + 18));
            elements.add(element((i + 1) + " - 4.5", 500, y, 640, y + 18));
        }

        DocumentLayout layout = analyzer.analyze(elements, List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(1);
        assertThat(layout.continuations()).isEmpty();
    }

    // ------------------------------------------------------------------------- tables

    @Test
    void tableWithHeaderBandIsClassifiedAsATable() {
        DocumentLayout layout = analyzer.analyze(equipmentTable(0, 0, 0), List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(1);
        LayoutRegion region = layout.regions().get(0);
        assertThat(region.kind()).isEqualTo(RegionKind.TABLE);
        assertThat(region.columnCount()).isEqualTo(3);
        assertThat(region.rowCount()).isEqualTo(4);
        assertThat(region.rows().get(0).textAt(0)).isEqualTo("Tag");
        assertThat(region.rows().get(2).textAt(1)).isEqualTo("Diaphragm Pump");
    }

    /**
     * The gap between a column of labels and its column of values is a perfectly good
     * vertical gutter. Cutting on it would leave a region of orphaned labels beside a
     * region of orphaned values and break every labelled sheet the pipeline already
     * handles, so aligned rows must suppress the cut.
     */
    @Test
    void keyValueBlockIsNotCutIntoOrphanedLabelsAndValues() {
        List<DocumentElement> elements = List.of(
                element("Tag Number:", 100, 100, 220, 118),
                element("P-101", 400, 100, 480, 118),
                element("Mfr:", 100, 140, 160, 158),
                element("Siemens", 400, 140, 500, 158),
                element("MAWP:", 100, 180, 180, 198),
                element("150 psi", 400, 180, 490, 198));

        DocumentLayout layout = analyzer.analyze(elements, List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(1);
        LayoutRegion region = layout.regions().get(0);
        assertThat(region.kind()).isEqualTo(RegionKind.KEY_VALUE);
        assertThat(region.columnCount()).isEqualTo(2);
        assertThat(region.rows().get(0).textAt(0)).isEqualTo("Tag Number:");
        assertThat(region.rows().get(0).textAt(1)).isEqualTo("P-101");
    }

    // --------------------------------------------------------------------- handwriting

    /**
     * Handwritten text has no shared baseline - no two words on a line agree on y. Banding
     * on vertical overlap rather than on equality is what keeps those words in one row.
     * The offsets are fixed rather than random so a failure is reproducible.
     */
    @Test
    void handwritingBaselineDriftStillBandsIntoRows() {
        DocumentLayout layout = analyzer.analyze(equipmentTable(5, -5, 4), List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(1);
        LayoutRegion region = layout.regions().get(0);
        assertThat(region.rowCount()).isEqualTo(4);
        assertThat(region.columnCount()).isEqualTo(3);
        assertThat(region.rows().get(0).cells()).hasSize(3);
    }

    // ------------------------------------------------------------------ irregular pages

    /**
     * Line gaps vary by more than half between consecutive lines. A fixed spacing threshold
     * would shatter this into several regions; a threshold relative to the document's own
     * median gap does not.
     */
    @Test
    void irregularLineSpacingDoesNotOverSegment() {
        double[] tops = {100, 140, 195, 233, 293, 335};
        List<DocumentElement> elements = new ArrayList<>();
        for (int i = 0; i < tops.length; i++) {
            elements.add(element("measurement line " + (i + 1), 100, tops[i], 400, tops[i] + 18));
        }

        DocumentLayout layout = analyzer.analyze(elements, List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(1);
        assertThat(layout.regions().get(0).rowCount()).isEqualTo(6);
    }

    /**
     * A full-width heading sits over two columns of text and occupies every x position,
     * hiding the gutter underneath it. Only alternating horizontal and vertical cuts finds
     * the columns: the heading has to be lifted into its own slab first.
     */
    @Test
    void headingAboveTwoColumnsOfProseIsCutIntoThreeRegions() {
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(element("QUARTERLY REPORT", 100, 50, 900, 75));
        for (double y : new double[]{200, 240, 280, 320}) {
            elements.add(element("left column prose", 100, y, 400, y + 18));
        }
        for (double y : new double[]{200, 260, 320}) {
            elements.add(element("right column prose", 500, y, 800, y + 18));
        }

        DocumentLayout layout = analyzer.analyze(elements, List.of(PAGE_ONE));

        assertThat(layout.regions()).hasSize(3);
        assertThat(layout.regions().get(0).rowCount()).isEqualTo(1);
        assertThat(layout.regions().get(1).rowCount()).isEqualTo(4);
        assertThat(layout.regions().get(2).rowCount()).isEqualTo(3);
    }

    // -------------------------------------------------------------------- multiple pages

    /**
     * Pages of different sizes must still be comparable, which is what normalizing to [0,1]
     * buys. Banding stays page-scoped so the last row of page 1 never joins the first row
     * of page 2.
     */
    @Test
    void multiPageDocumentKeepsPagesSeparateAndNormalizesCoordinates() {
        List<DocumentElement> elements = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double y = 100 + i * 40;
            elements.add(new DocumentElement(1, "left p1", "text", new BBox(100, y, 300, y + 18)));
            elements.add(new DocumentElement(1, "right p1", "text", new BBox(600, y, 800, y + 18)));
        }
        for (int i = 0; i < 3; i++) {
            double y = 120 + i * 48;
            elements.add(new DocumentElement(2, "left p2", "text", new BBox(80, y, 240, y + 22)));
            elements.add(new DocumentElement(2, "right p2", "text", new BBox(480, y, 640, y + 22)));
        }

        DocumentLayout layout = analyzer.analyze(elements, List.of(
                PAGE_ONE, new PageGeometry(2, 800, 1200)));

        assertThat(layout.regions()).hasSize(2);
        assertThat(layout.regions()).extracting(LayoutRegion::page).containsExactly(1, 2);
        assertThat(layout.regions()).allSatisfy(region -> {
            assertThat(region.rowCount()).isEqualTo(3);
            assertThat(region.bounds().xmin()).isBetween(0.0, 1.0);
            assertThat(region.bounds().xmax()).isBetween(0.0, 1.0);
            assertThat(region.bounds().ymax()).isBetween(0.0, 1.0);
        });
    }

    @Test
    void emptyInputProducesAnEmptyLayout() {
        assertThat(analyzer.analyze(List.of(), List.of(PAGE_ONE)).isEmpty()).isTrue();
        assertThat(analyzer.analyze(null, List.of(PAGE_ONE)).isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------------- fixtures

    private List<DocumentElement> twoColumnMeasurementList() {
        String[] left = {"3.5", "3.08", "3.64", "3.9", "3.22", "3.71"};
        String[] right = {"4.57", "3.67", "3.81", "4.02", "3.95", "4.11"};

        List<DocumentElement> elements = new ArrayList<>();
        elements.add(element("L", 100, 100, 120, 118));
        elements.add(element("R", 500, 100, 520, 118));
        for (int i = 0; i < 6; i++) {
            double y = 150 + i * 40;
            elements.add(element((i + 1) + " - " + left[i], 100, y, 220, y + 18));
            elements.add(element((i + 7) + " - " + right[i], 500, y, 640, y + 18));
        }
        return elements;
    }

    /** A three-column table; the offsets simulate handwritten baseline drift per column. */
    private List<DocumentElement> equipmentTable(double driftA, double driftB, double driftC) {
        String[][] rows = {
                {"Tag", "Type", "Pressure"},
                {"P-101", "Centrifugal Pump", "150 psi"},
                {"P-102", "Diaphragm Pump", "120 psi"},
                {"V-201", "Vessel", "90 psi"}};
        double[] columnStarts = {100, 300, 500};
        double[] drifts = {driftA, driftB, driftC};

        List<DocumentElement> elements = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            double baseY = 100 + rowIndex * 40;
            for (int column = 0; column < rows[rowIndex].length; column++) {
                double y = baseY + drifts[column];
                double x = columnStarts[column];
                elements.add(element(rows[rowIndex][column], x, y,
                        x + rows[rowIndex][column].length() * 8.0, y + 18));
            }
        }
        return elements;
    }

    private DocumentElement element(String text, double xmin, double ymin, double xmax, double ymax) {
        return new DocumentElement(1, text, "text", new BBox(xmin, ymin, xmax, ymax));
    }

    private List<Integer> leadingNumbersOf(LayoutRegion region) {
        List<Integer> numbers = new ArrayList<>();
        for (LayoutRow row : region.rows()) {
            if (row.cells().isEmpty()) {
                continue;
            }
            Matcher matcher = LEADING_NUMBER.matcher(row.cells().get(0).text());
            if (matcher.find()) {
                numbers.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return numbers;
    }
}
