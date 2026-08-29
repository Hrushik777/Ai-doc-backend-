package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.DocumentElement;
import com.example.ai_doc.domain.layout.DocumentLayout;
import com.example.ai_doc.domain.layout.PageGeometry;
import com.example.ai_doc.pipeline.layout.ColumnClusterer;
import com.example.ai_doc.pipeline.layout.ColumnGutterDetector;
import com.example.ai_doc.pipeline.layout.LayoutAnalyzer;
import com.example.ai_doc.pipeline.layout.RegionClassifier;
import com.example.ai_doc.pipeline.layout.RegionContinuationDetector;
import com.example.ai_doc.pipeline.layout.RowBander;
import com.example.ai_doc.pipeline.layout.VerticalSlabSplitter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import com.example.ai_doc.domain.mapping.MappedRecord;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mapping half of the two-column question. The layout offers both readings; these
 * tests pin down that the Excel template is what chooses between them, and that a table
 * resolves into one spreadsheet row per document row with no model call involved.
 */
class LayoutRecordMapperTest {

    private final RowBander rowBander = new RowBander();
    private final LayoutAnalyzer analyzer = new LayoutAnalyzer(
            rowBander,
            new VerticalSlabSplitter(rowBander),
            new ColumnGutterDetector(),
            new ColumnClusterer(),
            new RegionClassifier(),
            new RegionContinuationDetector());

    private final LayoutRecordMapper mapper =
            new LayoutRecordMapper(new HeaderFieldMapper(new HeaderNameNormalizer()));

    private static final PageGeometry PAGE = new PageGeometry(1, 1000, 1000);

    /**
     * The same page that produced two readings now has to produce twelve rows, because the
     * template asks for an index and a value - which is what the columns hold once they are
     * read downwards rather than across.
     */
    @Test
    void twoColumnListFillsAnIndexValueTemplateAsTwelveRows() {
        List<MappedRecord> records = mapper.mapLayout(
                template("Index", "Value"),
                analyzer.analyze(twoColumnMeasurementList(), List.of(PAGE)));

        assertThat(records).hasSize(12);
        assertThat(records.get(0).values()).containsExactlyInAnyOrderEntriesOf(Map.of(0, "1", 1, "3.5"));
        assertThat(records.get(6).values()).containsExactlyInAnyOrderEntriesOf(Map.of(0, "7", 1, "4.57"));
        assertThat(records.get(11).values()).containsExactlyInAnyOrderEntriesOf(Map.of(0, "12", 1, "4.11"));
    }

    /**
     * The identical document, against a template that names the two sides, must keep them
     * side by side instead. Nothing about the page changed - only the template did.
     */
    @Test
    void sameDocumentWithALeftRightTemplateKeepsTheColumnsPaired() {
        List<MappedRecord> records = mapper.mapLayout(
                template("L", "R"),
                analyzer.analyze(twoColumnMeasurementList(), List.of(PAGE)));

        assertThat(records).hasSize(6);
        assertThat(records.get(0).values()).containsExactlyInAnyOrderEntriesOf(Map.of(0, "1 - 3.5", 1, "7 - 4.57"));
        assertThat(records.get(5).values()).containsExactlyInAnyOrderEntriesOf(Map.of(0, "6 - 3.71", 1, "12 - 4.11"));
    }

    /** The headline case for tables: N document rows become N spreadsheet rows, no LLM. */
    @Test
    void tableWithAMatchingHeaderBandBecomesOneRecordPerRow() {
        List<MappedRecord> records = mapper.mapLayout(
                template("Tag", "Type", "Pressure"),
                analyzer.analyze(equipmentTable(400), List.of(PAGE)));

        assertThat(records).hasSize(3);
        assertThat(records.get(0).values()).containsEntry(0, "P-101").containsEntry(2, "150 psi");
        assertThat(records.get(2).values()).containsEntry(0, "V-201").containsEntry(1, "Vessel");
    }

    /** The compatibility guarantee: a labelled document still produces exactly one row. */
    @Test
    void keyValueDocumentStillProducesExactlyOneRecord() {
        List<MappedRecord> records = mapper.mapLayout(
                template("Tag Number", "Equipment Type", "Design Pressure"),
                analyzer.analyze(keyValueBlock(100), List.of(PAGE)));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).values()).containsExactlyInAnyOrderEntriesOf(
                Map.of(0, "P-101", 1, "Centrifugal Pump", 2, "120 psi"));
    }

    /**
     * A field stated once above a table describes every line under it, the way an invoice
     * number does, so it is copied onto each row rather than landing on only the first.
     */
    @Test
    void documentLevelLabelledValuesAreCopiedOntoEveryTableRow() {
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(element("Invoice:", 100, 100, 200, 118));
        elements.add(element("INV-42", 400, 100, 480, 118));
        elements.addAll(equipmentTable(400));

        List<MappedRecord> records = mapper.mapLayout(
                template("Invoice", "Tag", "Type", "Pressure"),
                analyzer.analyze(elements, List.of(PAGE)));

        assertThat(records).hasSize(3);
        assertThat(records).allSatisfy(record -> assertThat(record.values()).containsEntry(0, "INV-42"));
        assertThat(records.get(0).values()).containsEntry(1, "P-101");
        assertThat(records.get(2).values()).containsEntry(3, "90 psi");
    }

    @Test
    void anEmptyLayoutMapsToNoRecords() {
        assertThat(mapper.mapLayout(template("Tag"), DocumentLayout.empty())).isEmpty();
    }

    // ------------------------------------------------------------------------- fixtures

    private ExcelTemplateInfo template(String... headerNames) {
        List<ExcelColumn> headers = new ArrayList<>(headerNames.length);
        for (int i = 0; i < headerNames.length; i++) {
            headers.add(new ExcelColumn(i, headerNames[i]));
        }
        return new ExcelTemplateInfo("Sheet1", 0, 1, headers);
    }

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

    private List<DocumentElement> equipmentTable(double topY) {
        String[][] rows = {
                {"Tag", "Type", "Pressure"},
                {"P-101", "Centrifugal Pump", "150 psi"},
                {"P-102", "Diaphragm Pump", "120 psi"},
                {"V-201", "Vessel", "90 psi"}};
        double[] columnStarts = {100, 300, 500};

        List<DocumentElement> elements = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            double y = topY + rowIndex * 40;
            for (int column = 0; column < rows[rowIndex].length; column++) {
                double x = columnStarts[column];
                elements.add(element(rows[rowIndex][column], x, y,
                        x + rows[rowIndex][column].length() * 8.0, y + 18));
            }
        }
        return elements;
    }

    private List<DocumentElement> keyValueBlock(double topY) {
        String[][] pairs = {
                {"Tag Number:", "P-101"},
                {"Equipment Type:", "Centrifugal Pump"},
                {"Design Pressure:", "120 psi"}};

        List<DocumentElement> elements = new ArrayList<>();
        for (int i = 0; i < pairs.length; i++) {
            double y = topY + i * 40;
            elements.add(element(pairs[i][0], 100, y, 100 + pairs[i][0].length() * 8.0, y + 18));
            elements.add(element(pairs[i][1], 400, y, 400 + pairs[i][1].length() * 8.0, y + 18));
        }
        return elements;
    }

    private DocumentElement element(String text, double xmin, double ymin, double xmax, double ymax) {
        return new DocumentElement(1, text, "text", new BBox(xmin, ymin, xmax, ymax));
    }
}
