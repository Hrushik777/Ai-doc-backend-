package com.example.ai_doc.model.excel;

import java.util.List;

/** Metadata needed to write values back into the same template layout. */
public record ExcelTemplateInfo(
        String sheetName,
        int headerRowIndex,
        int dataRowIndex,
        List<ExcelColumn> headers) {

    public ExcelTemplateInfo {
        headers = List.copyOf(headers);
    }
}
