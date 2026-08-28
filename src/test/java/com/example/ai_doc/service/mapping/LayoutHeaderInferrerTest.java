package com.example.ai_doc.service.mapping;

import com.example.ai_doc.model.layout.BBox;
import com.example.ai_doc.model.layout.DocumentElement;
import com.example.ai_doc.model.layout.DocumentLayout;
import com.example.ai_doc.model.layout.PageGeometry;
import com.example.ai_doc.service.layout.ColumnClusterer;
import com.example.ai_doc.service.layout.ColumnGutterDetector;
import com.example.ai_doc.service.layout.LayoutAnalyzer;
import com.example.ai_doc.service.layout.RegionClassifier;
import com.example.ai_doc.service.layout.RegionContinuationDetector;
import com.example.ai_doc.service.layout.RowBander;
import com.example.ai_doc.service.layout.VerticalSlabSplitter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Column naming for documents that arrive without a template. Most documents state their
 * own headers, and reading them costs nothing - so these cases must never reach a model.
 */
class LayoutHeaderInferrerTest {

    private final RowBander rowBander = new RowBander();
    private final LayoutAnalyzer analyzer = new LayoutAnalyzer(
            rowBander,
            new VerticalSlabSplitter(rowBander),
            new ColumnGutterDetector(),
            new ColumnClusterer(),
            new RegionClassifier(),
            new RegionContinuationDetector());

    private final LayoutHeaderInferrer inferrer = new LayoutHeaderInferrer();

    private static final PageGeometry PAGE = new PageGeometry(1, 1000, 1000);

    @Test
    void aTableNamesItsOwnColumns() {
        List<String> headers = inferrer.infer(analyzer.analyze(equipmentTable(), List.of(PAGE)));

        assertThat(headers).containsExactly("Tag", "Type", "Pressure");
    }

    @Test
    void labelledPairsBecomeColumnsNamedAfterTheirLabels() {
        List<String> headers = inferrer.infer(analyzer.analyze(keyValueBlock(), List.of(PAGE)));

        assertThat(headers).containsExactly("Tag Number", "Equipment Type", "Design Pressure");
    }

    /**
     * A header band whose cells repeat is data that happened to land in row one, not a
     * description of the columns - naming two columns the same thing would collapse them.
     */
    @Test
    void aRepeatedFirstRowIsNotTreatedAsAHeaderBand() {
        List<DocumentElement> elements = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            double y = 100 + rowIndex * 40;
            elements.add(element("same", 100, y, 180, y + 18));
            elements.add(element("same", 400, y, 480, y + 18));
        }

        assertThat(inferrer.infer(analyzer.analyze(elements, List.of(PAGE)))).isEmpty();
    }

    @Test
    void anEmptyLayoutNamesNothing() {
        assertThat(inferrer.infer(DocumentLayout.empty())).isEmpty();
        assertThat(inferrer.infer(null)).isEmpty();
    }

    // ------------------------------------------------------------------------- fixtures

    private List<DocumentElement> equipmentTable() {
        String[][] rows = {
                {"Tag", "Type", "Pressure"},
                {"P-101", "Centrifugal Pump", "150 psi"},
                {"P-102", "Diaphragm Pump", "120 psi"},
                {"V-201", "Vessel", "90 psi"}};
        double[] columnStarts = {100, 300, 500};

        List<DocumentElement> elements = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            double y = 100 + rowIndex * 40;
            for (int column = 0; column < rows[rowIndex].length; column++) {
                double x = columnStarts[column];
                elements.add(element(rows[rowIndex][column], x, y,
                        x + rows[rowIndex][column].length() * 8.0, y + 18));
            }
        }
        return elements;
    }

    private List<DocumentElement> keyValueBlock() {
        String[][] pairs = {
                {"Tag Number:", "P-101"},
                {"Equipment Type:", "Centrifugal Pump"},
                {"Design Pressure:", "120 psi"}};

        List<DocumentElement> elements = new ArrayList<>();
        for (int i = 0; i < pairs.length; i++) {
            double y = 100 + i * 40;
            elements.add(element(pairs[i][0], 100, y, 100 + pairs[i][0].length() * 8.0, y + 18));
            elements.add(element(pairs[i][1], 400, y, 400 + pairs[i][1].length() * 8.0, y + 18));
        }
        return elements;
    }

    private DocumentElement element(String text, double xmin, double ymin, double xmax, double ymax) {
        return new DocumentElement(1, text, "text", new BBox(xmin, ymin, xmax, ymax));
    }
}
