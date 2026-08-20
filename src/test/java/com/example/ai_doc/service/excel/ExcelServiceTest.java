package com.example.ai_doc.service.excel;

import com.example.ai_doc.globalexception.InvalidExcelTemplateException;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.service.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelServiceTest {

    private final ExcelService excelService = new ExcelService(new ExcelTemplateValidator(), 0);

    @Test
    void readHeadersTrimsTextSkipsBlanksAndRetainsDuplicateColumns() throws IOException {
        MockMultipartFile template = xlsxTemplate(workbook -> {
            Row row = workbook.createSheet("Measurements").createRow(0);
            row.createCell(0).setCellValue(" Pressure ");
            row.createCell(1).setCellValue("   ");
            row.createCell(2).setCellValue("Temperature");
            row.createCell(4).setCellValue("Pressure");
        });

        ExcelTemplateInfo templateInfo = excelService.readHeaders(template);

        assertThat(templateInfo.sheetName()).isEqualTo("Measurements");
        assertThat(templateInfo.headerRowIndex()).isZero();
        assertThat(templateInfo.dataRowIndex()).isEqualTo(1);
        assertThat(templateInfo.headers())
                .extracting(column -> column.columnIndex() + ":" + column.headerName())
                .containsExactly("0:Pressure", "2:Temperature", "4:Pressure");
    }

    @Test
    void readHeadersRejectsMissingOrBlankHeaderRows() throws IOException {
        MockMultipartFile withoutRequestedRow = xlsxTemplate(workbook ->
                workbook.createSheet("Sheet1").createRow(0).createCell(0).setCellValue("Header"));
        MockMultipartFile blankHeaderRow = xlsxTemplate(workbook ->
                workbook.createSheet("Sheet1").createRow(0).createCell(0).setCellValue(" "));

        assertThatThrownBy(() -> excelService.readHeaders(withoutRequestedRow, 1))
                .isInstanceOf(InvalidExcelTemplateException.class)
                .hasMessageContaining("header row 1");
        assertThatThrownBy(() -> excelService.readHeaders(blankHeaderRow))
                .isInstanceOf(InvalidExcelTemplateException.class)
                .hasMessageContaining("does not contain any headers");
    }

    @Test
    void populateTemplateUpdatesOnlyMappedColumnsAndPreservesOtherCells() throws IOException {
        MockMultipartFile template = xlsxTemplate(workbook -> {
            Sheet sheet = workbook.createSheet("Measurements");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Pressure");
            headerRow.createCell(1).setCellValue("Temperature");
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(1).setCellValue("keep this value");
            sheet.createRow(3).createCell(3).setCellValue("unrelated cell");
        });

        ExcelTemplateInfo templateInfo = excelService.readHeaders(template);
        byte[] result = excelService.populateTemplate(template, templateInfo, Map.of(0, "125 PSI"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Measurements");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Pressure");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("125 PSI");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("keep this value");
            assertThat(sheet.getRow(3).getCell(3).getStringCellValue()).isEqualTo("unrelated cell");
        }
    }

    @Test
    void readHeadersRejectsNonXlsxTemplates() {
        MockMultipartFile template = new MockMultipartFile(
                "template", "template.pdf", "application/pdf", "not an excel workbook".getBytes());

        assertThatThrownBy(() -> excelService.readHeaders(template))
                .isInstanceOf(InvalidExcelTemplateException.class)
                .hasMessageContaining(".xlsx");
    }

    private MockMultipartFile xlsxTemplate(WorkbookCustomizer customizer) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            customizer.customize(workbook);
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "template",
                    "template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    @FunctionalInterface
    private interface WorkbookCustomizer {
        void customize(XSSFWorkbook workbook);
    }
}
