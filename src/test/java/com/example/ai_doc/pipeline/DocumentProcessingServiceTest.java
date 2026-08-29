package com.example.ai_doc.pipeline;

import com.example.ai_doc.TestFiles;
import com.example.ai_doc.api.error.DocumentProcessingException;
import com.example.ai_doc.api.error.EmptyFileException;
import com.example.ai_doc.api.error.NoExcelMappingsException;
import com.example.ai_doc.api.error.UnsupportedDocumentUnderstandingException;
import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.mapping.SemanticMapping;
import com.example.ai_doc.domain.result.BatchProcessedExcelFile;
import com.example.ai_doc.domain.result.ProcessedExcelFile;
import com.example.ai_doc.pipeline.excel.ExcelService;
import com.example.ai_doc.pipeline.mapping.HeaderFieldMapper;
import com.example.ai_doc.pipeline.mapping.HeaderNameNormalizer;
import com.example.ai_doc.pipeline.mapping.SemanticMappingService;
import com.example.ai_doc.pipeline.understanding.DocumentUnderstandingService;
import com.example.ai_doc.pipeline.validation.DocumentFileValidator;
import com.example.ai_doc.pipeline.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentProcessingServiceTest {

    @Test
    void coordinatesAProviderResultIntoTheMatchingTemplateCells() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        ExcelService excelService = new ExcelService(new ExcelTemplateValidator(), 0);
        HeaderFieldMapper headerFieldMapper = new HeaderFieldMapper(new HeaderNameNormalizer());
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(), excelService, understandingService, headerFieldMapper,
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("pressure", "125 PSI"),
                new ExtractedField("Temperature", "80 C")
        )));

        ProcessedExcelFile result = processingService.process(document, template);

        assertThat(result.filename()).isEqualTo("completed-document.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Row dataRow = workbook.getSheet("Measurements").getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("125 PSI");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("80 C");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("preserved");
        }
        verifyNoInteractions(semanticMappingService);
    }

    @Test
    void resolvesDuplicateSemanticColumnMappingsByKeepingTheHighestConfidenceValue() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplateWithPressureHeader();
        // Neither field name aliases or exactly matches "Maximum Allowable Working Pressure",
        // so both stay unmatched and this exercises the semantic-vs-semantic tie-break.
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure Reading", "150 psi"),
                new ExtractedField("Design Pressure", "160 psi")
        )));
        given(semanticMappingService.mapUnmatchedFields(any(), any())).willReturn(List.of(
                new SemanticMapping("field-0", "Pressure Reading", "150 psi", 0, 0.84, "First candidate"),
                new SemanticMapping("field-1", "Design Pressure", "160 psi", 0, 0.95, "Higher-confidence candidate")
        ));

        ProcessedExcelFile result = processingService.process(document, template);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            assertThat(workbook.getSheet("Measurements").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("160 psi");
        }
    }

    @Test
    void skipsTheSemanticLlmWhenEveryTemplateColumnIsAlreadyResolved() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        // Both template columns match deterministically, but plenty of prose fields stay
        // unmatched - the LLM still has nothing it could contribute.
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "125 PSI"),
                new ExtractedField("Temperature", "80 C"),
                new ExtractedField("Some prose heading", "unrelated narrative text"),
                new ExtractedField("Another caption", "more unrelated text")
        )));

        ProcessedExcelFile result = processingService.process(document, template);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Row dataRow = workbook.getSheet("Measurements").getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("125 PSI");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("80 C");
        }
        verifyNoInteractions(semanticMappingService);
    }

    @Test
    void sendsOnlyUnresolvedHeadersToTheSemanticLlm() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        // "Pressure" resolves deterministically; "Temperature" does not.
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "125 PSI"),
                new ExtractedField("Ambient Temp", "80 C")
        )));
        given(semanticMappingService.mapUnmatchedFields(any(), any())).willReturn(List.of());

        processingService.process(document, template);

        ArgumentCaptor<List<ExcelColumn>> headersCaptor = ArgumentCaptor.forClass(List.class);
        verify(semanticMappingService).mapUnmatchedFields(any(), headersCaptor.capture());
        assertThat(headersCaptor.getValue())
                .extracting(ExcelColumn::headerName)
                .containsExactly("Temperature");
    }

    @Test
    void keepsDeterministicValuesWhenTheSemanticStageFails() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        // "Pressure" resolves deterministically; "Ambient Temp" needs the LLM, which fails.
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "125 PSI"),
                new ExtractedField("Ambient Temp", "80 C")
        )));
        given(semanticMappingService.mapUnmatchedFields(any(), any()))
                .willThrow(new DocumentProcessingException("Semantic mapping model returned invalid JSON"));

        ProcessedExcelFile result = processingService.process(document, template);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Row dataRow = workbook.getSheet("Measurements").getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("125 PSI");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("preserved");
        }
    }

    @Test
    void reportsAnEmptyExtractionAsAnExtractionProblemNotAMatchingProblem() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        given(understandingService.extractFields(any()))
                .willReturn(new ExtractedDocumentData(List.of()));

        assertThatThrownBy(() -> processingService.process(document, template))
                .isInstanceOf(NoExcelMappingsException.class)
                .hasMessageContaining("Document understanding returned no fields");
        // Nothing was extracted, so there is nothing for the semantic stage to map.
        verifyNoInteractions(semanticMappingService);
    }

    @Test
    void reportsUnmatchedFieldsDistinctlyFromAnEmptyExtraction() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Totally unrelated", "some value")
        )));
        given(semanticMappingService.mapUnmatchedFields(any(), any())).willReturn(List.of());

        assertThatThrownBy(() -> processingService.process(document, template))
                .isInstanceOf(NoExcelMappingsException.class)
                .hasMessageContaining("1 fields extracted, none matched");
    }

    @Test
    void processBatchWritesSequentialRowsForEachDocument() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document1 = new MockMultipartFile(
                "documents", "scan1.pdf", "application/pdf", TestFiles.pdf("1"));
        MockMultipartFile document2 = new MockMultipartFile(
                "documents", "scan2.pdf", "application/pdf", TestFiles.pdf("2"));
        MockMultipartFile document3 = new MockMultipartFile(
                "documents", "scan3.pdf", "application/pdf", TestFiles.pdf("3"));
        MockMultipartFile template = xlsxTemplate();
        given(understandingService.extractFields(document1)).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "100 PSI"), new ExtractedField("Temperature", "70 C"))));
        given(understandingService.extractFields(document2)).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "110 PSI"), new ExtractedField("Temperature", "75 C"))));
        given(understandingService.extractFields(document3)).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "120 PSI"), new ExtractedField("Temperature", "78 C"))));

        BatchProcessedExcelFile result = processingService.processBatch(
                List.of(document1, document2, document3), template);

        assertThat(result.filename()).isEqualTo("completed-document.xlsx");
        assertThat(result.results()).extracting("filename", "success", "rowIndex")
                .containsExactly(
                        tuple("scan1.pdf", true, 1),
                        tuple("scan2.pdf", true, 2),
                        tuple("scan3.pdf", true, 3));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheet("Measurements");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("100 PSI");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("preserved");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("110 PSI");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("120 PSI");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Pressure");
        }
    }

    @Test
    void processBatchContinuesWhenOneDocumentFails() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document1 = new MockMultipartFile(
                "documents", "scan1.pdf", "application/pdf", TestFiles.pdf("1"));
        MockMultipartFile document2 = new MockMultipartFile(
                "documents", "scan2.pdf", "application/pdf", TestFiles.pdf("2"));
        MockMultipartFile document3 = new MockMultipartFile(
                "documents", "scan3.pdf", "application/pdf", TestFiles.pdf("3"));
        MockMultipartFile template = xlsxTemplate();
        given(understandingService.extractFields(document1)).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "100 PSI"), new ExtractedField("Temperature", "70 C"))));
        given(understandingService.extractFields(document2))
                .willThrow(new UnsupportedDocumentUnderstandingException("Unsupported content type"));
        given(understandingService.extractFields(document3)).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("Pressure", "120 PSI"), new ExtractedField("Temperature", "78 C"))));

        BatchProcessedExcelFile result = processingService.processBatch(
                List.of(document1, document2, document3), template);

        assertThat(result.results()).hasSize(3);
        assertThat(result.results().get(0).success()).isTrue();
        assertThat(result.results().get(1).success()).isFalse();
        assertThat(result.results().get(1).filename()).isEqualTo("scan2.pdf");
        assertThat(result.results().get(1).errorMessage()).isEqualTo("Unsupported content type");
        assertThat(result.results().get(2).success()).isTrue();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheet("Measurements");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("100 PSI");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue())
                    .isEqualTo("PROCESSING FAILED: Unsupported content type");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("120 PSI");
        }
    }

    @Test
    void processBatchThrowsWhenDocumentListIsEmpty() {
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                mock(DocumentUnderstandingService.class),
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                mock(SemanticMappingService.class));

        assertThatThrownBy(() -> processingService.processBatch(List.of(), new MockMultipartFile(
                "template", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1})))
                .isInstanceOf(EmptyFileException.class);
    }

    @Test
    void processBatchWithSingleDocumentMatchesSingleDocumentProcessing() throws IOException {
        DocumentUnderstandingService understandingService = mock(DocumentUnderstandingService.class);
        SemanticMappingService semanticMappingService = mock(SemanticMappingService.class);
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentFileValidator(),
                new ExcelService(new ExcelTemplateValidator(), 0),
                understandingService,
                new HeaderFieldMapper(new HeaderNameNormalizer()),
                semanticMappingService);
        MockMultipartFile document = new MockMultipartFile(
                "documents", "scan.pdf", "application/pdf", TestFiles.pdf("1-2"));
        MockMultipartFile template = xlsxTemplate();
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("pressure", "125 PSI"),
                new ExtractedField("Temperature", "80 C")
        )));

        BatchProcessedExcelFile result = processingService.processBatch(List.of(document), template);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            Row dataRow = workbook.getSheet("Measurements").getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("125 PSI");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("80 C");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("preserved");
        }
        verifyNoInteractions(semanticMappingService);
    }

    private MockMultipartFile xlsxTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Measurements");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Pressure");
            headerRow.createCell(1).setCellValue("Temperature");
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(2).setCellValue("preserved");
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "template",
                    "template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile xlsxTemplateWithPressureHeader() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Measurements");
            sheet.createRow(0).createCell(0).setCellValue("Maximum Allowable Working Pressure");
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "template",
                    "template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
