package com.example.ai_doc.service.mapping;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps extracted fields only when their normalized names exactly match a template header. */
@Component
public class HeaderFieldMapper {

    private final HeaderNameNormalizer headerNameNormalizer;

    public HeaderFieldMapper(HeaderNameNormalizer headerNameNormalizer) {
        this.headerNameNormalizer = headerNameNormalizer;
    }

    public Map<Integer, String> mapToColumns(ExcelTemplateInfo templateInfo,
                                             ExtractedDocumentData extractedDocumentData) {
        Map<String, String> valuesByNormalizedName = new LinkedHashMap<>();
        for (ExtractedField field : extractedDocumentData.fields()) {
            String normalizedName = headerNameNormalizer.normalize(field.name());
            if (!normalizedName.isEmpty() && field.value() != null) {
                valuesByNormalizedName.putIfAbsent(normalizedName, field.value());
            }
        }

        Map<Integer, String> valuesByColumn = new LinkedHashMap<>();
        for (ExcelColumn header : templateInfo.headers()) {
            String value = valuesByNormalizedName.get(headerNameNormalizer.normalize(header.headerName()));
            if (value != null) {
                valuesByColumn.put(header.columnIndex(), value);
            }
        }
        return valuesByColumn;
    }
}
