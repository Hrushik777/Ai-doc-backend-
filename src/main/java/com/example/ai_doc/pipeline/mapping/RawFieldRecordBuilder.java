package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.mapping.MappedRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes out everything that was read from the document, one row per element, with the
 * geometry intact.
 *
 * <p>This is the floor the pipeline cannot fall through. Every other path decides what a
 * value <em>means</em> - which column it belongs in, whether a band is a header, whether
 * two visual columns are one list - and each of those decisions can be wrong or can decline
 * to be made, leaving the caller with an empty workbook and nothing to work from. This path
 * decides nothing. It preserves the field name, the value, the element type, the page and
 * the rectangle, so a person can see what the document actually contained and sort it out
 * themselves.
 */
@Component
public class RawFieldRecordBuilder {

    /** Column order of the generated sheet. */
    public static final List<String> HEADERS =
            List.of("Field", "Value", "Type", "Page", "X", "Y", "Width", "Height");

    private static final int FIELD = 0;
    private static final int VALUE = 1;
    private static final int TYPE = 2;
    private static final int PAGE = 3;
    private static final int X = 4;
    private static final int Y = 5;
    private static final int WIDTH = 6;
    private static final int HEIGHT = 7;

    public List<MappedRecord> build(ExtractedDocumentData extractedDocumentData) {
        if (extractedDocumentData == null || extractedDocumentData.fields().isEmpty()) {
            return List.of();
        }

        List<MappedRecord> records = new ArrayList<>(extractedDocumentData.fields().size());

        for (ExtractedField field : extractedDocumentData.fields()) {
            Map<Integer, String> values = new LinkedHashMap<>();
            put(values, FIELD, field.name());
            put(values, VALUE, field.value());
            put(values, TYPE, field.sourceType());
            put(values, PAGE, field.pageNumber() == null ? null : String.valueOf(field.pageNumber()));
            put(values, X, format(field.x()));
            put(values, Y, format(field.y()));
            put(values, WIDTH, format(field.width()));
            put(values, HEIGHT, format(field.height()));

            if (!values.isEmpty()) {
                records.add(MappedRecord.of(values));
            }
        }

        return records;
    }

    private void put(Map<Integer, String> values, int column, String value) {
        if (value != null && !value.isBlank()) {
            values.put(column, value);
        }
    }

    /** Coordinates are normalized to [0,1]; four decimals is well under a pixel on any page. */
    private String format(Double coordinate) {
        return coordinate == null ? null : String.format(java.util.Locale.ROOT, "%.4f", coordinate);
    }
}
