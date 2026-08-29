package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.domain.mapping.DeterministicMappingResult;
import com.example.ai_doc.domain.mapping.IndexedExtractedField;
import com.example.ai_doc.domain.mapping.MappingSource;
import com.example.ai_doc.domain.mapping.ResolvedFieldMapping;
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

        // Normalize the headers once per document instead of once per (field, header) pair.
        // Matching then costs one hash lookup per field rather than a full scan of the
        // header list, without changing which columns a field resolves to.
        Map<String, List<ExcelColumn>> columnsByNormalizedHeader =
                indexHeadersByNormalizedName(templateInfo.headers());

        List<ExtractedField> fields = extractedDocumentData.fields();
        for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
            ExtractedField field = fields.get(fieldIndex);
            boolean matched = addExactMatches(fieldIndex, field, columnsByNormalizedHeader, mappingsByColumn);
            if (!matched) {
                unmatchedFields.add(new IndexedExtractedField(fieldIndex, field));
            }
        }

        return new DeterministicMappingResult(mappingsByColumn, unmatchedFields);
    }

    /**
     * Normalized template header name to the columns carrying it. Public so the layout-aware
     * mapper resolves headers through exactly the same normalization and alias rules as the
     * flat path - two matching implementations would drift, and a header that resolved on one
     * path but not the other would be all but impossible to explain.
     */
    public Map<String, List<ExcelColumn>> indexHeadersByNormalizedName(List<ExcelColumn> headers) {
        // Duplicate header names are legal and every matching column must still receive the
        // value, so each normalized name maps to the list of columns that carry it, kept in
        // template order.
        Map<String, List<ExcelColumn>> columnsByNormalizedHeader = new LinkedHashMap<>();
        for (ExcelColumn header : headers) {
            String normalizedHeaderName = headerNameNormalizer.normalize(header.headerName());
            if (normalizedHeaderName.isEmpty()) {
                continue;
            }
            columnsByNormalizedHeader
                    .computeIfAbsent(normalizedHeaderName, key -> new ArrayList<>(1))
                    .add(header);
        }
        return columnsByNormalizedHeader;
    }

    private boolean addExactMatches(
            int fieldIndex,
            ExtractedField field,
            Map<String, List<ExcelColumn>> columnsByNormalizedHeader,
            Map<Integer, ResolvedFieldMapping> mappingsByColumn) {

        if (field.value() == null) {
            return false;
        }

        String normalizedFieldName =
                headerNameNormalizer.normalize(field.name());

        if (normalizedFieldName.isEmpty()) {
            return false;
        }

        List<ExcelColumn> matchingColumns = columnsByNormalizedHeader.get(matchKey(normalizedFieldName));

        if (matchingColumns == null) {
            return false;
        }

        for (ExcelColumn header : matchingColumns) {

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

        return true;
    }

    /** Normalizes a name and resolves any known abbreviation, for header lookup. */
    public String matchKey(String name) {
        String normalized = headerNameNormalizer.normalize(name);
        return HEADER_ALIASES.getOrDefault(normalized, normalized);
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
