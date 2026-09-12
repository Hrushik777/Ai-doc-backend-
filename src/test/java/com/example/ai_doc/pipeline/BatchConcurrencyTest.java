package com.example.ai_doc.pipeline;

import com.example.ai_doc.TestFiles;
import com.example.ai_doc.api.error.ExternalAiServiceException;
import com.example.ai_doc.domain.layout.BBox;
import com.example.ai_doc.domain.layout.DocumentElement;
import com.example.ai_doc.domain.layout.PageGeometry;
import com.example.ai_doc.domain.layout.ParsedDocument;
import com.example.ai_doc.domain.result.BatchItemResult;
import com.example.ai_doc.domain.result.BatchProcessedExcelFile;
import com.example.ai_doc.pipeline.excel.ExcelService;
import com.example.ai_doc.pipeline.mapping.HeaderFieldMapper;
import com.example.ai_doc.pipeline.mapping.HeaderNameNormalizer;
import com.example.ai_doc.pipeline.mapping.SemanticMappingService;
import com.example.ai_doc.pipeline.understanding.DocumentUnderstandingService;
import com.example.ai_doc.pipeline.validation.DocumentFileValidator;
import com.example.ai_doc.pipeline.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * A batch reads its documents concurrently and writes them one at a time.
 *
 * <p>That split is the whole design, and both halves can fail quietly. If the writes lost their
 * order, rows would land under the wrong filenames - a workbook that looks entirely plausible and
 * is wrong. If a failing document stopped taking its place in the sequence, every document after
 * it would shift up a row.
 *
 * <p>So the timings here are deliberately skewed: the first document is made the slowest and the
 * last the fastest, meaning completion order is the reverse of document order. A batch that wrote
 * results as they arrived would pass with equal timings and fail here.
 */
class BatchConcurrencyTest {

    @Test
    void writesDocumentsInSubmittedOrderEvenWhenLaterOnesFinishFirst() throws IOException {
        DocumentUnderstandingService understanding = mock(DocumentUnderstandingService.class);
        List<MultipartFile> documents = documents(3);

        for (int index = 0; index < documents.size(); index++) {
            int tagNumber = index + 1;
            long delay = (documents.size() - index) * 40L;
            given(understanding.parse(documents.get(index))).willAnswer(invocation -> {
                Thread.sleep(delay);
                return tableFor("P-10" + tagNumber);
            });
        }

        BatchProcessedExcelFile result =
                service(understanding, 3).processBatch(documents, template());

        assertThat(rowsOf(result)).containsExactly(
                "P-101-1", "P-101-2", "P-101-3",
                "P-102-1", "P-102-2", "P-102-3",
                "P-103-1", "P-103-2", "P-103-3");
        assertThat(result.results()).extracting(BatchItemResult::filename)
                .containsExactly("doc-0.pdf", "doc-1.pdf", "doc-2.pdf");
    }

    /** Concurrency is an implementation detail: the workbook must match the sequential one. */
    @Test
    void producesTheSameWorkbookAsSequentialProcessing() throws IOException {
        assertThat(rowsOf(runWith(3))).isEqualTo(rowsOf(runWith(1)));
    }

    /**
     * One document failing must not take the batch with it, and must not shuffle the others. Its
     * failure row belongs exactly where the document was, or every later row is attributed wrongly.
     */
    @Test
    void aFailingDocumentKeepsItsPlaceInTheSequence() throws IOException {
        DocumentUnderstandingService understanding = mock(DocumentUnderstandingService.class);
        List<MultipartFile> documents = documents(3);

        given(understanding.parse(documents.get(0))).willReturn(tableFor("P-101"));
        given(understanding.parse(documents.get(1)))
                .willThrow(new ExternalAiServiceException("NVIDIA Nemotron Parse request failed"));
        given(understanding.parse(documents.get(2))).willAnswer(invocation -> {
            // Finishes long after the failure, so an implementation that wrote on completion
            // would put this row above the failure rather than below it.
            Thread.sleep(120);
            return tableFor("P-103");
        });

        BatchProcessedExcelFile result =
                service(understanding, 3).processBatch(documents, template());

        List<String> rows = rowsOf(result);
        assertThat(rows.get(0)).isEqualTo("P-101-1");
        assertThat(rows.get(2)).isEqualTo("P-101-3");
        assertThat(rows.get(3)).startsWith("PROCESSING FAILED");
        assertThat(rows.get(4)).isEqualTo("P-103-1");

        assertThat(result.results()).extracting(BatchItemResult::success)
                .containsExactly(true, false, true);
    }

    /** No more documents may be read at once than configured - that bound is what caps memory. */
    @Test
    void neverReadsMoreDocumentsAtOnceThanConfigured() throws IOException {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        DocumentUnderstandingService understanding = mock(DocumentUnderstandingService.class);
        List<MultipartFile> documents = documents(8);
        given(understanding.parse(any())).willAnswer(invocation -> {
            peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            try {
                Thread.sleep(30);
                return tableFor("P-101");
            } finally {
                inFlight.decrementAndGet();
            }
        });

        service(understanding, 2).processBatch(documents, template());

        assertThat(peak.get()).isLessThanOrEqualTo(2);
    }

    // ------------------------------------------------------------------------- helpers

    private BatchProcessedExcelFile runWith(int concurrency) throws IOException {
        DocumentUnderstandingService understanding = mock(DocumentUnderstandingService.class);
        List<MultipartFile> documents = documents(4);
        for (int index = 0; index < documents.size(); index++) {
            given(understanding.parse(documents.get(index))).willReturn(tableFor("P-10" + (index + 1)));
        }
        return service(understanding, concurrency).processBatch(documents, template());
    }

    private DocumentProcessingService service(DocumentUnderstandingService understanding,
                                              int batchConcurrency) {
        HeaderFieldMapper headerFieldMapper = new HeaderFieldMapper(new HeaderNameNormalizer());
        return new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understanding,
                headerFieldMapper,
                mock(SemanticMappingService.class),
                new com.example.ai_doc.pipeline.understanding.PdfDocumentRenderer(),
                defaultAnalyzer(),
                new com.example.ai_doc.pipeline.mapping.LayoutRecordMapper(headerFieldMapper),
                new com.example.ai_doc.pipeline.understanding.ParsedDocumentFlattener(),
                new com.example.ai_doc.pipeline.mapping.LayoutHeaderInferrer()::infer,
                new com.example.ai_doc.pipeline.mapping.RawFieldRecordBuilder(),
                DocumentProcessingService.NoTemplateMode.INFERRED_HEADERS,
                com.example.ai_doc.domain.excel.ExcelWriteMode.FILL_THEN_APPEND,
                batchConcurrency);
    }

    /** Mirrors DocumentProcessingService's own default wiring for the layout stage. */
    private static com.example.ai_doc.pipeline.layout.LayoutAnalyzer defaultAnalyzer() {
        var rowBander = new com.example.ai_doc.pipeline.layout.RowBander();
        return new com.example.ai_doc.pipeline.layout.LayoutAnalyzer(
                rowBander,
                new com.example.ai_doc.pipeline.layout.VerticalSlabSplitter(rowBander),
                new com.example.ai_doc.pipeline.layout.ColumnGutterDetector(),
                new com.example.ai_doc.pipeline.layout.ColumnClusterer(),
                new com.example.ai_doc.pipeline.layout.RegionClassifier(),
                new com.example.ai_doc.pipeline.layout.RegionContinuationDetector());
    }

    private List<MultipartFile> documents(int count) {
        List<MultipartFile> documents = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            documents.add(new MockMultipartFile(
                    "documents", "doc-" + index + ".pdf", "application/pdf",
                    TestFiles.pdf("doc-" + index)));
        }
        return documents;
    }

    /**
     * A table whose header band matches the template, so it resolves structurally without the
     * semantic stage. Geometry and row count mirror the fixture the other batch tests use: the
     * layout analyzer classifies a region by its shape, and two rows is not yet a table.
     *
     * <p>Each document contributes three rows, all tagged with its own marker, so the order rows
     * land in is directly observable.
     */
    private ParsedDocument tableFor(String tag) {
        String[][] rows = {
                {"Tag", "Type", "Pressure"},
                {tag + "-1", "Centrifugal Pump", "150 psi"},
                {tag + "-2", "Diaphragm Pump", "120 psi"},
                {tag + "-3", "Vessel", "90 psi"}};
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

    private MockMultipartFile template() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Equipment");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Tag");
            header.createCell(1).setCellValue("Type");
            header.createCell(2).setCellValue("Pressure");
            workbook.write(out);
            return new MockMultipartFile("template", "template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    /** First-column values of every written data row, in sheet order. */
    private List<String> rowsOf(BatchProcessedExcelFile result) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheet("Equipment");
            List<String> values = new ArrayList<>();
            for (int row = 1; row <= sheet.getLastRowNum(); row++) {
                var cell = sheet.getRow(row) == null ? null : sheet.getRow(row).getCell(0);
                if (cell != null && !cell.getStringCellValue().isBlank()) {
                    values.add(cell.getStringCellValue());
                }
            }
            return values;
        }
    }
}
