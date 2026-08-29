package com.example.ai_doc.domain.mapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact matches plus only the fields that still require semantic evaluation. */
public record DeterministicMappingResult(
        Map<Integer, ResolvedFieldMapping> mappingsByColumn,
        List<IndexedExtractedField> unmatchedFields) {

    public DeterministicMappingResult {
        // Not Map.copyOf: that returns an immutable map with an arbitrary iteration order,
        // which silently made the order columns are visited in vary from run to run. The
        // resolved values are keyed by column so the workbook was still correct, but an
        // ordering that shifts underneath you is not something to leave in a pipeline.
        mappingsByColumn = Collections.unmodifiableMap(new LinkedHashMap<>(mappingsByColumn));
        unmatchedFields = List.copyOf(unmatchedFields);
    }
}
