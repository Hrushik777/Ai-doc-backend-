package com.example.ai_doc.model.mapping;

import java.util.List;
import java.util.Map;

/** Exact matches plus only the fields that still require semantic evaluation. */
public record DeterministicMappingResult(
        Map<Integer, ResolvedFieldMapping> mappingsByColumn,
        List<IndexedExtractedField> unmatchedFields) {

    public DeterministicMappingResult {
        mappingsByColumn = Map.copyOf(mappingsByColumn);
        unmatchedFields = List.copyOf(unmatchedFields);
    }
}
