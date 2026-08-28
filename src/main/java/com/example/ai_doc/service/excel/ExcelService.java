package com.example.ai_doc.service.excel;

import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.globalexception.InvalidExcelTemplateException;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.service.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    private static final int INITIAL_WORKBOOK_BUFFER_BYTES = 64 * 1024;

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
