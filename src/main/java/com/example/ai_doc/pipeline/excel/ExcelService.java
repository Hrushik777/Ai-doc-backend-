package com.example.ai_doc.pipeline.excel;

import com.example.ai_doc.api.error.DocumentProcessingException;
import com.example.ai_doc.api.error.InvalidExcelTemplateException;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.domain.excel.ExcelWriteMode;
import com.example.ai_doc.pipeline.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    private static final int INITIAL_WORKBOOK_BUFFER_BYTES = 64 * 1024;

    private static final String GENERATED_SHEET_NAME = "Extracted Data";

    private final ExcelTemplateValidator excelTemplateValidator;
    private final int defaultHeaderRowIndex;

    public ExcelService(ExcelTemplateValidator excelTemplateValidator,
                        @Value("${app.excel.header-row-index:0}") int defaultHeaderRowIndex) {
        this.excelTemplateValidator = excelTemplateValidator;
        this.defaultHeaderRowIndex = defaultHeaderRowIndex;
    }

    public ExcelTemplateInfo readHeaders(MultipartFile excelFile) {
        return readHeaders(excelFile, defaultHeaderRowIndex);
    }

    public ExcelTemplateInfo readHeaders(MultipartFile excelFile, int headerRowIndex) {
        try (Workbook workbook = openWorkbook(excelFile)) {
            return readHeaders(workbook, headerRowIndex);
        } catch (InvalidExcelTemplateException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new InvalidExcelTemplateException("Excel template could not be opened as an XLSX workbook", exception);
        }
    }

    /**
     * Opens the given Excel file as a POI {@link Workbook}. The caller owns the returned
     * workbook and is responsible for closing it (try-with-resources) once all reads/writes
     * against it are done - this lets a single open workbook be reused across multiple
     * {@link #readHeaders(Workbook, int)} / {@link #writeRow} calls instead of re-parsing
     * the file from bytes every time.
     */
    public Workbook openWorkbook(MultipartFile excelFile) {
        excelTemplateValidator.validate(excelFile);

        try (InputStream inputStream = excelFile.getInputStream()) {
            return WorkbookFactory.create(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw new InvalidExcelTemplateException("Excel template could not be opened as an XLSX workbook", exception);
        }
    }

    public ExcelTemplateInfo readHeaders(Workbook workbook) {
        return readHeaders(workbook, defaultHeaderRowIndex);
    }

    public ExcelTemplateInfo readHeaders(Workbook workbook, int headerRowIndex) {
        if (headerRowIndex < 0) {
            throw new InvalidExcelTemplateException("Header row index cannot be negative");
        }

        if (workbook.getNumberOfSheets() == 0) {
            throw new InvalidExcelTemplateException("Excel template does not contain a worksheet");
        }

        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            throw new InvalidExcelTemplateException(
                    "Excel template does not contain header row " + headerRowIndex);
        }

        List<ExcelColumn> headers = extractHeaders(headerRow);
        if (headers.isEmpty()) {
            throw new InvalidExcelTemplateException("Excel template header row does not contain any headers");
        }

        return new ExcelTemplateInfo(sheet.getSheetName(), headerRowIndex, headerRowIndex + 1, headers);
    }

    /**
     * Writes one row of values into an already-open workbook. Unlike {@link #populateTemplate},
     * the row to write is an explicit parameter rather than always {@code templateInfo.dataRowIndex()},
     * which lets a caller write multiple rows (e.g. one per document in a batch) into the same
     * open workbook before serializing it once.
     */
    public void writeRow(Workbook workbook,
                         ExcelTemplateInfo templateInfo,
                         int rowIndex,
                         Map<Integer, String> valuesByColumn) {
        Sheet sheet = workbook.getSheet(templateInfo.sheetName());
        if (sheet == null) {
            throw new InvalidExcelTemplateException("The template worksheet could not be found");
        }

        Row dataRow = sheet.getRow(rowIndex);
        if (dataRow == null) {
            dataRow = sheet.createRow(rowIndex);
        }

        for (Map.Entry<Integer, String> entry : valuesByColumn.entrySet()) {
            if (entry.getKey() < 0) {
                throw new DocumentProcessingException("Excel column index cannot be negative");
            }

            Cell cell = dataRow.getCell(entry.getKey());
            if (cell == null) {
                cell = dataRow.createCell(entry.getKey());
            }
            cell.setCellValue(entry.getValue());
        }
    }

    /**
     * Writes consecutive rows starting at {@code startRowIndex} and returns how many were
     * written.
     *
     * <p>One document is not one row. A table or a list produces a record per row, and a
     * batch has to know how far the previous document advanced before it places the next
     * one. A labelled key-value document still produces exactly one record, so this is a
     * superset of the previous single-row behaviour rather than a change to it.
     */
    public int writeRows(Workbook workbook,
                         ExcelTemplateInfo templateInfo,
                         int startRowIndex,
                         List<Map<Integer, String>> records) {
        int rowIndex = startRowIndex;
        for (Map<Integer, String> record : records) {
            writeRow(workbook, templateInfo, rowIndex, record);
            rowIndex++;
        }
        return rowIndex - startRowIndex;
    }

    /**
     * Builds an empty workbook around a header row that was inferred rather than supplied,
     * for the case where the caller sent a document but no template.
     */
    public SynthesizedTemplate createWorkbook(List<String> headerNames) {
        if (headerNames == null || headerNames.isEmpty()) {
            throw new InvalidExcelTemplateException("Cannot build a workbook without any headers");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(GENERATED_SHEET_NAME);
        Row headerRow = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        List<ExcelColumn> headers = new ArrayList<>(headerNames.size());
        for (int columnIndex = 0; columnIndex < headerNames.size(); columnIndex++) {
            String headerName = headerNames.get(columnIndex);
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(headerName);
            cell.setCellStyle(headerStyle);
            headers.add(new ExcelColumn(columnIndex, headerName));
        }

        return new SynthesizedTemplate(
                workbook,
                new ExcelTemplateInfo(GENERATED_SHEET_NAME, 0, 1, headers));
    }

    /** A generated workbook and the template description that matches it. */
    public record SynthesizedTemplate(Workbook workbook, ExcelTemplateInfo templateInfo) {
    }

    /**
     * Places records into the sheet without destroying anything already in it.
     *
     * <p>Replaces the old "write from the header row down" behaviour, which assumed the
     * template was empty and silently overwrote the user's own rows when it was not.
     *
     * @return where the output went and what could not be placed
     */
    public WriteOutcome writeRecords(Workbook workbook,
                                     ExcelTemplateInfo templateInfo,
                                     List<Map<Integer, String>> records,
                                     ExcelWriteMode mode) {
        return writeRecords(workbook, templateInfo, records, mode,
                lastDataRow(sheetOf(workbook, templateInfo), templateInfo));
    }

    /**
     * @param gapFillBoundary the last row eligible to be gap-filled - the sheet's last data
     *                        row as it stood <em>before</em> this run wrote anything.
     *                        Rows added during the run must be excluded: a batch writes a
     *                        one-cell failure marker for a document that could not be read,
     *                        and that row has the same shape as a user's partly-filled one,
     *                        so the next document would pour its values into the error
     *                        instead of appending beneath it.
     */
    public WriteOutcome writeRecords(Workbook workbook,
                                     ExcelTemplateInfo templateInfo,
                                     List<Map<Integer, String>> records,
                                     ExcelWriteMode mode,
                                     int gapFillBoundary) {

        Sheet sheet = sheetOf(workbook, templateInfo);

        if (mode == ExcelWriteMode.OVERWRITE) {
            int written = writeRows(workbook, templateInfo, templateInfo.dataRowIndex(), records);
            return new WriteOutcome(templateInfo.dataRowIndex(), 0, written, 0);
        }

        int lastDataRow = lastDataRow(sheet, templateInfo);
        Deque<Map<Integer, String>> pending = new ArrayDeque<>(records);

        int firstRowTouched = -1;
        int filled = 0;
        int skippedValues = 0;

        if (mode == ExcelWriteMode.FILL_THEN_APPEND) {
            int scanTo = Math.min(lastDataRow, gapFillBoundary);
            for (int rowIndex = templateInfo.dataRowIndex(); rowIndex <= scanTo && !pending.isEmpty(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (!hasBlankMappedCell(row, templateInfo, pending.peek())) {
                    continue;
                }

                skippedValues += fillBlanks(row, pending.poll());
                filled++;
                if (firstRowTouched < 0) {
                    firstRowTouched = rowIndex;
                }
            }
        }

        int appendAt = lastDataRow + 1;
        int appended = 0;
        Row styleSource = lastDataRow >= templateInfo.dataRowIndex() ? sheet.getRow(lastDataRow) : null;

        while (!pending.isEmpty()) {
            int rowIndex = appendAt + appended;
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            writeInto(row, pending.poll(), styleSource);
            appended++;
            if (firstRowTouched < 0) {
                firstRowTouched = rowIndex;
            }
        }

        return new WriteOutcome(
                firstRowTouched < 0 ? templateInfo.dataRowIndex() : firstRowTouched,
                filled, appended, skippedValues);
    }

    /** Where a document's output landed, and what would not fit. */
    public record WriteOutcome(int firstRowIndex, int rowsFilled, int rowsAppended, int skippedValues) {

        public int rowsWritten() {
            return rowsFilled + rowsAppended;
        }
    }

    /**
     * The last row carrying content in one of the template's own header columns.
     *
     * <p>Deliberately not {@code getLastRowNum()}: that returns the last row that
     * physically exists, which may be a blank row left behind by an edit, or a note typed
     * well below the table in a column the template knows nothing about. Appending after
     * either of those would leave a gulf of empty rows in the middle of the sheet.
     *
     * @return {@code dataRowIndex - 1} when the sheet holds no data at all
     */
    public int lastDataRow(Sheet sheet, ExcelTemplateInfo templateInfo) {
        int last = templateInfo.dataRowIndex() - 1;
        for (int rowIndex = templateInfo.dataRowIndex(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (ExcelColumn header : templateInfo.headers()) {
                if (!isBlank(row, header.columnIndex())) {
                    last = rowIndex;
                    break;
                }
            }
        }
        return last;
    }

    /**
     * Whether a cell holds nothing a user would consider content. A formula is content
     * even before it is evaluated, so it is never blank - overwriting one would replace a
     * calculation with a literal.
     */
    public boolean isBlank(Row row, int columnIndex) {
        if (row == null) {
            return true;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().strip().isEmpty();
        }
        return false;
    }

    private boolean hasBlankMappedCell(Row row, ExcelTemplateInfo templateInfo, Map<Integer, String> record) {
        for (Integer columnIndex : record.keySet()) {
            if (isBlank(row, columnIndex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes only into the blank cells of an existing row.
     *
     * @return how many of the record's values had nowhere to go, because the cell they
     *         belonged in was already populated. They are reported rather than forced in:
     *         the user's own value wins, but losing ours silently would be worse.
     */
    private int fillBlanks(Row row, Map<Integer, String> record) {
        int skipped = 0;
        for (Map.Entry<Integer, String> entry : record.entrySet()) {
            requireNonNegative(entry.getKey());
            if (!isBlank(row, entry.getKey())) {
                skipped++;
                continue;
            }
            Cell cell = row.getCell(entry.getKey());
            if (cell == null) {
                cell = row.createCell(entry.getKey());
            }
            cell.setCellValue(entry.getValue());
        }
        return skipped;
    }

    /**
     * Writes a whole record into a fresh row, taking each cell's formatting from the row
     * above so appended data does not stand out from what is already there.
     */
    private void writeInto(Row row, Map<Integer, String> record, Row styleSource) {
        for (Map.Entry<Integer, String> entry : record.entrySet()) {
            requireNonNegative(entry.getKey());
            Cell cell = row.getCell(entry.getKey());
            if (cell == null) {
                cell = row.createCell(entry.getKey());
            }
            cell.setCellValue(entry.getValue());

            if (styleSource != null) {
                Cell template = styleSource.getCell(entry.getKey());
                if (template != null) {
                    cell.setCellStyle(template.getCellStyle());
                }
            }
        }
    }

    private Sheet sheetOf(Workbook workbook, ExcelTemplateInfo templateInfo) {
        Sheet sheet = workbook.getSheet(templateInfo.sheetName());
        if (sheet == null) {
            throw new InvalidExcelTemplateException("The template worksheet could not be found");
        }
        return sheet;
    }

    private void requireNonNegative(int columnIndex) {
        if (columnIndex < 0) {
            throw new DocumentProcessingException("Excel column index cannot be negative");
        }
    }

    public byte[] serialize(Workbook workbook) {
        // A default ByteArrayOutputStream starts at 32 bytes and copies its whole contents on
        // every growth; starting at a typical workbook size removes that copy chain.
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(INITIAL_WORKBOOK_BUFFER_BYTES)) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to serialize Excel workbook", exception);
        }
    }

    public byte[] populateTemplate(MultipartFile excelFile,
                                   ExcelTemplateInfo templateInfo,
                                   Map<Integer, String> valuesByColumn) {
        try (Workbook workbook = openWorkbook(excelFile)) {
            writeRow(workbook, templateInfo, templateInfo.dataRowIndex(), valuesByColumn);
            return serialize(workbook);
        } catch (InvalidExcelTemplateException | DocumentProcessingException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new DocumentProcessingException("Failed to populate Excel template", exception);
        }
    }

    private List<ExcelColumn> extractHeaders(Row headerRow) {
        DataFormatter formatter = new DataFormatter();
        List<ExcelColumn> headers = new ArrayList<>();

        for (int columnIndex = headerRow.getFirstCellNum();
             columnIndex >= 0 && columnIndex < headerRow.getLastCellNum();
             columnIndex++) {
            Cell cell = headerRow.getCell(columnIndex);
            if (cell == null) {
                continue;
            }

            String headerName = formatter.formatCellValue(cell).strip();
            if (!headerName.isEmpty()) {
                headers.add(new ExcelColumn(columnIndex, headerName));
            }
        }

        return headers;
    }
}
