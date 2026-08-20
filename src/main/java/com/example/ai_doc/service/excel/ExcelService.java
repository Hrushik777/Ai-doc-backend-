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
        excelTemplateValidator.validate(excelFile);

        if (headerRowIndex < 0) {
            throw new InvalidExcelTemplateException("Header row index cannot be negative");
        }

        try (InputStream inputStream = excelFile.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
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
        } catch (InvalidExcelTemplateException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new InvalidExcelTemplateException("Excel template could not be opened as an XLSX workbook", exception);
        }
    }

    public byte[] populateTemplate(MultipartFile excelFile,
                                   ExcelTemplateInfo templateInfo,
                                   Map<Integer, String> valuesByColumn) {
        excelTemplateValidator.validate(excelFile);

        try (InputStream inputStream = excelFile.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet(templateInfo.sheetName());
            if (sheet == null) {
                throw new InvalidExcelTemplateException("The template worksheet could not be found");
            }

            Row dataRow = sheet.getRow(templateInfo.dataRowIndex());
            if (dataRow == null) {
                dataRow = sheet.createRow(templateInfo.dataRowIndex());
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

            workbook.write(outputStream);
            return outputStream.toByteArray();
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
