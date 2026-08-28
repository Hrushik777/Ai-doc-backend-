package com.example.ai_doc.model.explain;

import com.example.ai_doc.model.excel.ExcelColumn;

import java.util.List;

/**
 * The completed workbook plus the evidence behind it: what was read from the
 * document, and which template column each value was placed in and why.
 *
 * <p>Produced by the same pipeline as the binary endpoint, so the workbook here
 * is byte-identical to what {@code /process} would have returned.
 */
public record ProcessExplanation(
        String filename,
        String workbookBase64,
        List<ExcelColumn> headers,
        List<ExplainedField> fields,
        List<ExplainedMapping> mappings,
        List<String> pageImagesBase64) {

    public ProcessExplanation {
        headers = List.copyOf(headers);
        fields = List.copyOf(fields);
        mappings = List.copyOf(mappings);
        pageImagesBase64 = List.copyOf(pageImagesBase64);
    }
}
