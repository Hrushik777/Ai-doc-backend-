package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.mapping.MappedRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The last-resort path, reached when no column could be named.
 *
 * <p>What it writes matters more than it looks: a caller lands here having asked for a
 * spreadsheet of their document and been told the columns could not be worked out. Answering
 * that with the element type and four columns of normalized coordinates buried the content
 * they came for, so geometry is now something to opt into rather than the default.
 */
class RawFieldRecordBuilderTest {

    @Test
    void writesOnlyTheReadableColumnsByDefault() {
        RawFieldRecordBuilder builder = new RawFieldRecordBuilder();

        assertThat(builder.headers()).containsExactly("Field", "Value", "Page");

        List<MappedRecord> records = builder.build(oneField());
        assertThat(records).hasSize(1);
        assertThat(records.get(0).values())
                .containsEntry(0, "Summary")
                .containsEntry(1, "an unlabelled line")
                .containsEntry(2, "1");
        // No Type, and no geometry trailing behind it.
        assertThat(records.get(0).values()).doesNotContainKeys(3, 4, 5, 6, 7);
    }

    @Test
    void writesTypeAndGeometryWhenAskedTo() {
        RawFieldRecordBuilder builder = new RawFieldRecordBuilder(true);

        assertThat(builder.headers())
                .containsExactly("Field", "Value", "Page", "Type", "X", "Y", "Width", "Height");

        MappedRecord record = builder.build(oneField()).get(0);
        assertThat(record.values())
                .containsEntry(2, "1")
                .containsEntry(3, "Text")
                .containsEntry(4, "0.1000")
                .containsEntry(5, "0.2500");
    }

    /** Column order has to line up with the header row, whichever set is in use. */
    @Test
    void everyWrittenColumnIndexExistsInTheHeaderRow() {
        for (RawFieldRecordBuilder builder :
                List.of(new RawFieldRecordBuilder(), new RawFieldRecordBuilder(true))) {

            int headerCount = builder.headers().size();
            assertThat(builder.build(oneField()).get(0).values().keySet())
                    .allSatisfy(column -> assertThat(column).isLessThan(headerCount));
        }
    }

    @Test
    void producesNothingForADocumentThatYieldedNoFields() {
        assertThat(new RawFieldRecordBuilder().build(new ExtractedDocumentData(List.of()))).isEmpty();
        assertThat(new RawFieldRecordBuilder().build(null)).isEmpty();
    }

    private ExtractedDocumentData oneField() {
        return new ExtractedDocumentData(List.of(new ExtractedField(
                "Summary", "an unlabelled line", null, 1, 0.1, 0.25, 0.6, 0.02, "Text",
                "an unlabelled line")));
    }
}
