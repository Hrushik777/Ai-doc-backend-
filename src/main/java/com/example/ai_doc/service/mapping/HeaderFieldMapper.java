package com.example.ai_doc.service.mapping;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.model.mapping.DeterministicMappingResult;
import com.example.ai_doc.model.mapping.IndexedExtractedField;
import com.example.ai_doc.model.mapping.MappingSource;
import com.example.ai_doc.model.mapping.ResolvedFieldMapping;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Maps extracted fields only when their normalized names exactly match a template header. */
@Component
public class HeaderFieldMapper {

    private final HeaderNameNormalizer headerNameNormalizer;

    public HeaderFieldMapper(HeaderNameNormalizer headerNameNormalizer) {
        this.headerNameNormalizer = headerNameNormalizer;
    }
    private static final Map<String, String> HEADER_ALIASES = Map.of(
            "mfr", "manufacturer",
            "mawp", "maximum allowable working pressure"
    );

    public Map<Integer, String> mapToColumns(ExcelTemplateInfo templateInfo,
                                             ExtractedDocumentData extractedDocumentData) {
        Map<Integer, String> valuesByColumn = new LinkedHashMap<>();
        for (ResolvedFieldMapping mapping : findExactMatches(templateInfo, extractedDocumentData)
                .mappingsByColumn().values()) {
            valuesByColumn.put(mapping.columnIndex(), mapping.value());
        }
        return valuesByColumn;
    }

    public DeterministicMappingResult findExactMatches(ExcelTemplateInfo templateInfo,
                                                        ExtractedDocumentData extractedDocumentData) {
        Map<Integer, ResolvedFieldMapping> mappingsByColumn = new LinkedHashMap<>();
        List<IndexedExtractedField> unmatchedFields = new ArrayList<>();

        for (int fieldIndex = 0; fieldIndex < extractedDocumentData.fields().size(); fieldIndex++) {
            ExtractedField field = extractedDocumentData.fields().get(fieldIndex);
            boolean matched = addExactMatches(fieldIndex, field, templateInfo.headers(), mappingsByColumn);
            if (!matched) {
                unmatchedFields.add(new IndexedExtractedField(fieldIndex, field));
            }
        }

        return new DeterministicMappingResult(mappingsByColumn, unmatchedFields);
    }

    private boolean addExactMatches(
            int fieldIndex,
            ExtractedField field,
            List<ExcelColumn> headers,
            Map<Integer, ResolvedFieldMapping> mappingsByColumn) {

        String normalizedFieldName =
                headerNameNormalizer.normalize(field.name());

        if (normalizedFieldName.isEmpty() || field.value() == null) {
            return false;
        }

        String comparableFieldName =
                HEADER_ALIASES.getOrDefault(
                        normalizedFieldName,
                        normalizedFieldName
                );

        boolean matched = false;

        for (ExcelColumn header : headers) {

            String normalizedHeaderName =
                    headerNameNormalizer.normalize(header.headerName());

            if (comparableFieldName.equals(normalizedHeaderName)) {

                matched = true;

                ResolvedFieldMapping candidate =
                        new ResolvedFieldMapping(
                                fieldIndex,
                                header.columnIndex(),
                                field.value(),
                                1.0,
                                MappingSource.DETERMINISTIC
                        );

                mappingsByColumn.merge(
                        header.columnIndex(),
                        candidate,
                        this::choosePreferredMapping
                );
            }
        }

        return matched;
    }

    private ResolvedFieldMapping choosePreferredMapping(ResolvedFieldMapping existing,
                                                         ResolvedFieldMapping candidate) {
        // Equal-confidence deterministic collisions keep the first field from document order.
        if (candidate.confidence() > existing.confidence()
                || (candidate.confidence() == existing.confidence()
                && candidate.fieldIndex() < existing.fieldIndex())) {
            return candidate;
        }
        return existing;
    }
}
