package com.example.ai_doc.model.excel;

/** A non-blank header cell in an Excel worksheet. */
public record ExcelColumn(int columnIndex, String headerName) {
}
