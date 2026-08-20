package com.example.ai_doc.service.processing;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.mapping.SemanticMapping;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
                "document", "scan.pdf", "application/pdf", new byte[]{1, 2});
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
                "document", "scan.pdf", "application/pdf", new byte[]{1, 2});
        MockMultipartFile template = xlsxTemplateWithPressureHeader();
        given(understandingService.extractFields(any())).willReturn(new ExtractedDocumentData(List.of(
                new ExtractedField("MAWP", "150 psi"),
                new ExtractedField("Design Pressure", "160 psi")
        )));
        given(semanticMappingService.mapUnmatchedFields(any(), any())).willReturn(List.of(
                new SemanticMapping(0, 0, 0.84, "First candidate"),
                new SemanticMapping(1, 0, 0.95, "Higher-confidence candidate")
        ));

        ProcessedExcelFile result = processingService.process(document, template);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            assertThat(workbook.getSheet("Measurements").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("160 psi");
        }
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
