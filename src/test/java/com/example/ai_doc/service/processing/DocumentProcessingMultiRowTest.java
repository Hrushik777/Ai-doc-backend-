package com.example.ai_doc.service.processing;

import com.example.ai_doc.model.explain.ExplainedMapping;
import com.example.ai_doc.model.explain.ProcessExplanation;
import com.example.ai_doc.model.layout.BBox;
import com.example.ai_doc.model.layout.DocumentElement;
import com.example.ai_doc.model.layout.PageGeometry;
import com.example.ai_doc.model.layout.ParsedDocument;
import com.example.ai_doc.model.processing.BatchProcessedExcelFile;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.service.excel.ExcelService;
import com.example.ai_doc.service.mapping.HeaderFieldMapper;
import com.example.ai_doc.service.mapping.HeaderNameNormalizer;
import com.example.ai_doc.service.mapping.SemanticMappingService;
import com.example.ai_doc.service.understanding.DocumentUnderstandingService;
import com.example.ai_doc.service.validation.DocumentFileValidator;
import com.example.ai_doc.service.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * End-to-end proof that one document can now fill many spreadsheet rows.
 *
 * <p>Everything below the parse model is real - the layout analysis, the mapper, and POI
 * writing an actual workbook - so these assertions are made against the bytes a caller
 * would download. Only the parse model itself is stubbed, with the positioned elements it
 * would have returned.
 */
class DocumentProcessingMultiRowTest {

    private final DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
    private final SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);

    private final DocumentProcessingService processingService = new DocumentProcessingService(
            new DocumentFileValidator(),
            new ExcelService(new ExcelTemplateValidator(), 0),
            understandingService,
            new HeaderFieldMapper(new HeaderNameNormalizer()),
            semanticMappingService);

    @Test
    void aTableProducesOneSpreadsheetRowPerTableRowWithoutCallingTheSemanticStage() throws IOException {
        given(understandingService.parse(any())).willReturn(equipmentTable());

        ProcessedExcelFile result = processingService.process(pdf("scan.pdf"), equipmentTemplate());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheet("Equipment");

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("P-101");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("150 psi");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("P-102");
            assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Vessel");
            assertThat(sheet.getRow(4)).isNull();
        }

        // The table resolved structurally, so every column was filled before the fallback
        // could have contributed anything.
        verifyNoInteractions(semanticMappingService);
    }

    /**
     * A batch used to advance exactly one row per document. Now that a document can occupy
     * several, the next one has to start below all of them - otherwise each document
     * overwrites the tail of the one before it.
     */
    @Test
    void batchAdvancesPastEveryRowTheProvingDocumentWrote() throws IOException {
        MockMultipartFile first = pdf("first.pdf");
        MockMultipartFile second = pdf("second.pdf");

        given(understandingService.parse(first)).willReturn(equipmentTable());
        given(understandingService.parse(second)).willReturn(equipmentTable());

        BatchProcessedExcelFile result =
                processingService.processBatch(List.of(first, second), equipmentTemplate());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheet("Equipment");

            // Three rows from the first document, then three more from the second.
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("V-201");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("P-101");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("V-201");
            assertThat(sheet.getRow(7)).isNull();
        }

        assertThat(result.results()).hasSize(2);
        assertThat(result.results().get(0).rowIndex()).isEqualTo(1);
        assertThat(result.results().get(1).rowIndex()).isEqualTo(4);
    }

    /**
     * No template at all. The document states its own column names, so the workbook is
     * built around them and filled in the same pass - and because the names were read from
     * the layout rather than guessed, this path costs no extra model call either.
     */
    @Test
    void aDocumentWithNoTemplateGetsAWorkbookBuiltFromItsOwnHeaders() throws IOException {
        given(understandingService.parse(any())).willReturn(equipmentTable());

        ProcessedExcelFile result = processingService.process(pdf("scan.pdf"), null);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Tag");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Type");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Pressure");

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("P-101");
            assertThat(sheet.getRow(3).getCell(2).getStringCellValue()).isEqualTo("90 psi");
            assertThat(sheet.getRow(4)).isNull();
        }
    }

    /**
     * The provenance view has to keep up with the multi-row spine. A structurally mapped
     * value has no entry in the flat extracted-field list, so if the explanation is built
     * only from the flat mappings it comes back empty for precisely the table documents the
     * layout analysis was added to handle.
     */
    @Test
    void everyStructuralCellIsExplainedWithTheRowAndRectangleItCameFrom() throws IOException {
        given(understandingService.parse(any())).willReturn(equipmentTable());

        ProcessExplanation explanation = processingService.explain(pdf("scan.pdf"), equipmentTemplate());

        // Three table rows, three columns each.
        assertThat(explanation.mappings()).hasSize(9);
        assertThat(explanation.mappings()).allSatisfy(mapping -> {
            assertThat(mapping.source()).isEqualTo("STRUCTURAL");
            assertThat(mapping.pageNumber()).isEqualTo(1);
            assertThat(mapping.width()).isGreaterThan(0.0);
            assertThat(mapping.height()).isGreaterThan(0.0);
        });
        assertThat(explanation.mappings())
                .extracting(ExplainedMapping::rowIndex)
                .containsOnly(0, 1, 2);

        ExplainedMapping firstTag = explanation.mappings().stream()
                .filter(mapping -> "P-101".equals(mapping.value()))
                .findFirst()
                .orElseThrow();

        assertThat(firstTag.rowIndex()).isZero();
        assertThat(firstTag.columnIndex()).isZero();
        assertThat(firstTag.reason()).contains("matched the template header");
        // Read from the left edge of the page, well above the middle.
        assertThat(firstTag.x()).isLessThan(0.2);
        assertThat(firstTag.y()).isLessThan(0.2);
    }

    // ------------------------------------------------------------------------- fixtures

    private ParsedDocument equipmentTable() {
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
                elements.add(new DocumentElement(1, rows[rowIndex][column], "text",
                        new BBox(x, y, x + rows[rowIndex][column].length() * 8.0, y + 18)));
            }
        }

        return new ParsedDocument(elements, List.of(new PageGeometry(1, 1000, 1000)));
    }

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("document", filename, "application/pdf", new byte[]{1, 2});
    }

    private MockMultipartFile equipmentTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Equipment");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Tag");
            headerRow.createCell(1).setCellValue("Type");
            headerRow.createCell(2).setCellValue("Pressure");
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "template",
                    "template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
