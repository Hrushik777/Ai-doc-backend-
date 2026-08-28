package com.example.ai_doc.model.mapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One row destined for the spreadsheet: the values by column, and - for values the layout
 * resolved - where on the page each one was read from.
 *
 * <p>A column present in {@code values} but absent from {@code origins} was filled by the
 * flat deterministic or semantic stage, whose provenance is recorded separately against the
 * extracted field it came from.
 */
public record MappedRecord(Map<Integer, String> values, Map<Integer, CellOrigin> origins) {

    public MappedRecord {
        values = Map.copyOf(values);
        origins = Map.copyOf(origins);
    }

    public static MappedRecord of(Map<Integer, String> values) {
        return new MappedRecord(values, Map.of());
    }

    /** Adds values this record does not already carry, leaving their origin unrecorded. */
    public MappedRecord withDefaults(Map<Integer, String> defaults) {
        if (defaults.isEmpty()) {
            return this;
        }
        Map<Integer, String> merged = new LinkedHashMap<>(values);
        defaults.forEach(merged::putIfAbsent);
        return new MappedRecord(merged, origins);
    }
}
