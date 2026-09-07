package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.mapping.MappedRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * decides nothing, so a person can see what the document actually contained and sort it out
 * themselves.
 *
 * <p>By default it writes the field, its value and its page, and stops there. It used to
 * write the element type and the bounding rectangle too, which is diagnostic output: a
 * caller who lands here has already had headers declined, and answering that with four
 * columns of normalized coordinates buries the content they came for. Set
 * {@code app.raw-fields.include-geometry=true} to get the geometry back for debugging.
 */
@Component
public class RawFieldRecordBuilder {

    /**
     * The readable columns. This is what a person opening the workbook actually wants: what
     * was read, and where to find it in the document.
     */
    private static final List<String> READABLE_HEADERS = List.of("Field", "Value", "Page");

    /**
     * The readable columns plus the geometry the parse model reported. Useful when the
     * question is "why did it read the document that way", useless when the question is
     * "what does the document say" - which is why it is no longer the default.
     */
    private static final List<String> DIAGNOSTIC_HEADERS =
            List.of("Field", "Value", "Page", "Type", "X", "Y", "Width", "Height");

    private static final int FIELD = 0;
    private static final int VALUE = 1;
    private static final int PAGE = 2;
    private static final int TYPE = 3;
    private static final int X = 4;
    private static final int Y = 5;
    private static final int WIDTH = 6;
    private static final int HEIGHT = 7;

    private final boolean includeGeometry;

    public RawFieldRecordBuilder() {
        this(false);
    }

    @Autowired
    public RawFieldRecordBuilder(@Value("${app.raw-fields.include-geometry:false}") boolean includeGeometry) {
        this.includeGeometry = includeGeometry;
    }

    /** Column order of the generated sheet. */
    public List<String> headers() {
        return includeGeometry ? DIAGNOSTIC_HEADERS : READABLE_HEADERS;
    }

    public List<MappedRecord> build(ExtractedDocumentData extractedDocumentData) {
        if (extractedDocumentData == null || extractedDocumentData.fields().isEmpty()) {
            return List.of();
        }

        List<MappedRecord> records = new ArrayList<>(extractedDocumentData.fields().size());

        for (ExtractedField field : extractedDocumentData.fields()) {
            Map<Integer, String> values = new LinkedHashMap<>();
            put(values, FIELD, field.name());
            put(values, VALUE, field.value());
            put(values, PAGE, field.pageNumber() == null ? null : String.valueOf(field.pageNumber()));

            if (includeGeometry) {
                put(values, TYPE, field.sourceType());
                put(values, X, format(field.x()));
                put(values, Y, format(field.y()));
                put(values, WIDTH, format(field.width()));
                put(values, HEIGHT, format(field.height()));
            }

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
