package com.example.ai_doc.pipeline.excel;

import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.domain.excel.ExcelWriteMode;
import com.example.ai_doc.pipeline.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Writing into a spreadsheet that already holds the user's own data.
 *
 * <p>The pipeline used to write from the header row down unconditionally, so a template
 * with fifty rows in it lost the first N of them without warning. These tests pin down
 * that nothing already present is destroyed.
 */
class ExcelWriteModeTest {

    private final ExcelService excelService = new ExcelService(new ExcelTemplateValidator(), 0);

    @Test
    void appendsBelowExistingDataWithoutTouchingIt() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "P-101", "Pump");
            dataRow(sheet, 2, "P-102", "Pump");
            dataRow(sheet, 3, "V-201", "Vessel");

            ExcelService.WriteOutcome outcome = write(workbook, List.of(
                    Map.of(0, "P-301", 1, "Screw Pump"),
                    Map.of(0, "V-401", 1, "Separator")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(text(sheet, 1, 0)).isEqualTo("P-101");
            assertThat(text(sheet, 3, 0)).isEqualTo("V-201");
            assertThat(text(sheet, 4, 0)).isEqualTo("P-301");
            assertThat(text(sheet, 5, 1)).isEqualTo("Separator");
            assertThat(outcome.rowsAppended()).isEqualTo(2);
            assertThat(outcome.rowsFilled()).isZero();
            assertThat(outcome.firstRowIndex()).isEqualTo(4);
        }
    }

    /** The in-between case: a row exists but one of its cells was left empty. */
    @Test
    void fillsABlankCellInAnExistingRowWithoutAddingARow() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "P-101", null);

            ExcelService.WriteOutcome outcome =
                    write(workbook, List.of(Map.of(1, "Pump")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(text(sheet, 1, 0)).isEqualTo("P-101");
            assertThat(text(sheet, 1, 1)).isEqualTo("Pump");
            assertThat(sheet.getRow(2)).isNull();
            assertThat(outcome.rowsFilled()).isEqualTo(1);
            assertThat(outcome.rowsAppended()).isZero();
        }
    }

    /**
     * When a record carries a value for a cell the user has already filled, theirs wins and
     * ours is counted rather than dropped in silence.
     */
    @Test
    void keepsTheExistingValueOnCollisionAndReportsIt() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "USER TYPED THIS", null);

            ExcelService.WriteOutcome outcome = write(workbook,
                    List.of(Map.of(0, "P-999", 1, "Pump")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(text(sheet, 1, 0)).isEqualTo("USER TYPED THIS");
            assertThat(text(sheet, 1, 1)).isEqualTo("Pump");
            assertThat(outcome.skippedValues()).isEqualTo(1);
        }
    }

    /**
     * getLastRowNum() would report row 9 here. Appending after it would leave a gulf of
     * empty rows between the table and the new data.
     */
    @Test
    void ignoresContentBelowTheTableInAColumnTheTemplateDoesNotKnow() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "P-101", "Pump");
            sheet.createRow(9).createCell(7).setCellValue("a note someone left");

            write(workbook, List.of(Map.of(0, "P-201", 1, "Screw Pump")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(text(sheet, 2, 0)).isEqualTo("P-201");
            assertThat(text(sheet, 9, 7)).isEqualTo("a note someone left");
        }
    }

    @Test
    void ignoresTrailingRowsThatExistButAreEmpty() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "P-101", "Pump");
            sheet.createRow(2);
            sheet.createRow(3).createCell(0).setCellValue("   ");

            write(workbook, List.of(Map.of(0, "P-201", 1, "Screw Pump")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(text(sheet, 2, 0)).isEqualTo("P-201");
        }
    }

    /** A formula is content before it is evaluated; replacing one with a literal loses it. */
    @Test
    void treatsAFormulaCellAsPopulated() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("P-101");
            row.createCell(1).setCellFormula("CONCATENATE(A2,\"-x\")");

            ExcelService.WriteOutcome outcome =
                    write(workbook, List.of(Map.of(1, "Pump")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(sheet.getRow(1).getCell(1).getCellFormula()).isEqualTo("CONCATENATE(A2,\"-x\")");
            // Nowhere to fill, so the record went below instead of overwriting the formula.
            assertThat(outcome.rowsAppended()).isEqualTo(1);
        }
    }

    @Test
    void appendedRowsInheritTheFormattingOfTheRowAbove() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            CellStyle centred = workbook.createCellStyle();
            centred.setAlignment(HorizontalAlignment.CENTER);

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("P-101");
            row.getCell(0).setCellStyle(centred);

            write(workbook, List.of(Map.of(0, "P-201")), ExcelWriteMode.FILL_THEN_APPEND);

            assertThat(sheet.getRow(2).getCell(0).getCellStyle().getAlignment())
                    .isEqualTo(HorizontalAlignment.CENTER);
        }
    }

    @Test
    void appendOnlyLeavesRowsWithGapsAlone() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "P-101", null);

            write(workbook, List.of(Map.of(1, "Pump")), ExcelWriteMode.APPEND_ONLY);

            assertThat(excelService.isBlank(sheet.getRow(1), 1)).isTrue();
            assertThat(text(sheet, 2, 1)).isEqualTo("Pump");
        }
    }

    @Test
    void overwriteReproducesTheOriginalBehaviour() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = sheetWithHeaders(workbook);
            dataRow(sheet, 1, "P-101", "Pump");

            write(workbook, List.of(Map.of(0, "REPLACED", 1, "ALSO REPLACED")), ExcelWriteMode.OVERWRITE);

            assertThat(text(sheet, 1, 0)).isEqualTo("REPLACED");
            assertThat(text(sheet, 1, 1)).isEqualTo("ALSO REPLACED");
        }
    }

    // ------------------------------------------------------------------------- helpers

    private ExcelService.WriteOutcome write(XSSFWorkbook workbook,
                                            List<Map<Integer, String>> records,
                                            ExcelWriteMode mode) {
        ExcelTemplateInfo templateInfo = excelService.readHeaders(workbook);
        return excelService.writeRecords(workbook, templateInfo, records, mode);
    }

    private Sheet sheetWithHeaders(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("Equipment");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Tag");
        header.createCell(1).setCellValue("Type");
        return sheet;
    }

    private void dataRow(Sheet sheet, int rowIndex, String tag, String type) {
        Row row = sheet.createRow(rowIndex);
        if (tag != null) {
            row.createCell(0).setCellValue(tag);
        }
        if (type != null) {
            row.createCell(1).setCellValue(type);
        }
    }

    private String text(Sheet sheet, int rowIndex, int columnIndex) {
        return sheet.getRow(rowIndex).getCell(columnIndex).getStringCellValue();
    }
}
