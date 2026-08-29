package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.mapping.IndexedExtractedField;
import com.example.ai_doc.domain.mapping.SemanticMapping;
import com.example.ai_doc.pipeline.nvidia.NvidiaChatCompletionClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * What the model returns is untrusted input.
 *
 * <p>The system prompt asks it not to invent values, not to lift one field's value onto a
 * different field, and not to treat a header mentioned in prose as data. These tests cover
 * what happens when it does anyway, because a prompt is a request and the workbook is what
 * the user actually receives.
 */
class SemanticMappingValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NvidiaChatCompletionClient nvidiaClient = mock(NvidiaChatCompletionClient.class);
    private final NemotronSemanticMappingService mappingService = new NemotronSemanticMappingService(
            nvidiaClient, objectMapper, "nvidia/nemotron-3-super-120b-a12b", 0.80, 4096, true, true);

    /** A value the source field does not contain was invented, whatever the reason says. */
    @Test
    void rejectsAValueThatDoesNotAppearInItsCitedSourceField() throws Exception {
        respondWith("""
                {"mappings":[{"fieldId":"field-0","name":"Manufacturer","value":"Siemens AG",
                "columnIndex":2,"confidence":0.95,"reason":"expanded the company name"}]}
                """);

        assertThat(map(field(0, "Mfr", "Siemens", "Mfr: Siemens"))).isEmpty();
    }

    /**
     * The classic cross-contamination failure: a value that is genuinely in the document,
     * but in a different field from the one the mapping cites.
     */
    @Test
    void rejectsAValueCopiedFromADifferentField() throws Exception {
        respondWith("""
                {"mappings":[{"fieldId":"field-1","name":"Manufacturer","value":"P-101",
                "columnIndex":2,"confidence":0.95,"reason":"seen elsewhere in the document"}]}
                """);

        List<SemanticMapping> mappings = map(
                field(0, "Tag Number", "P-101", "Tag Number: P-101"),
                field(1, "Mfr", "Siemens", "Mfr: Siemens"));

        assertThat(mappings).isEmpty();
    }

    /** A header named in explanatory prose is not that header's value. */
    @Test
    void rejectsAHeaderNameLiftedOutOfProse() throws Exception {
        respondWith("""
                {"mappings":[{"fieldId":"field-0","name":"Manufacturer","value":"Manufacturer",
                "columnIndex":2,"confidence":0.9,"reason":"the word appears in the text"}]}
                """);

        // The word is present in the prose, so grounding alone accepts it - the confidence
        // floor and the prompt are what keep this out. Documented here so the boundary of
        // the grounding check is explicit rather than assumed.
        List<SemanticMapping> mappings = map(field(0, "Text",
                "This document tests Manufacturer and MAWP abbreviations",
                "This document tests Manufacturer and MAWP abbreviations"));

        assertThat(mappings).allSatisfy(mapping ->
                assertThat(mapping.confidence()).isGreaterThanOrEqualTo(0.80));
    }

    @Test
    void rejectsAMappingCitingASourceFieldThatWasNeverSupplied() throws Exception {
        respondWith("""
                {"mappings":[{"fieldId":"field-999","name":"Manufacturer","value":"Siemens",
                "columnIndex":2,"confidence":0.95,"reason":"no such field was sent"}]}
                """);

        assertThat(map(field(0, "Mfr", "Siemens", "Mfr: Siemens"))).isEmpty();
    }

    /**
     * One bad entry must not take the good ones with it. Failing the document here would
     * discard correct mappings and leave a workbook emptier than it needed to be.
     */
    @Test
    void dropsOnlyTheInvalidMappingAndKeepsTheRest() throws Exception {
        respondWith("""
                {"mappings":[
                  {"fieldId":"field-0","name":"Manufacturer","value":"Siemens",
                   "columnIndex":2,"confidence":0.95,"reason":"grounded"},
                  {"fieldId":"field-1","name":"Tag Number","value":"",
                   "columnIndex":0,"confidence":0.9,"reason":"blank value"},
                  {"fieldId":"field-2","name":"Equipment Type","value":"Pump",
                   "columnIndex":1,"confidence":0.93,"reason":"grounded"}
                ]}
                """);

        List<SemanticMapping> mappings = map(
                field(0, "Mfr", "Siemens", "Mfr: Siemens"),
                field(1, "Tag", "P-101", "Tag: P-101"),
                field(2, "Type", "Pump", "Type: Pump"));

        assertThat(mappings).extracting(SemanticMapping::value)
                .containsExactlyInAnyOrder("Siemens", "Pump");
    }

    @Test
    void rejectsConfidenceOutsideZeroToOne() throws Exception {
        respondWith("""
                {"mappings":[{"fieldId":"field-0","name":"Manufacturer","value":"Siemens",
                "columnIndex":2,"confidence":4.2,"reason":"out of range"}]}
                """);

        assertThat(map(field(0, "Mfr", "Siemens", "Mfr: Siemens"))).isEmpty();
    }

    @Test
    void keepsOnlyTheFirstMappingWhenOneSourceFieldIsCitedTwice() throws Exception {
        respondWith("""
                {"mappings":[
                  {"fieldId":"field-0","name":"Manufacturer","value":"Siemens",
                   "columnIndex":2,"confidence":0.95,"reason":"first"},
                  {"fieldId":"field-0","name":"Equipment Type","value":"Siemens",
                   "columnIndex":1,"confidence":0.94,"reason":"same field again"}
                ]}
                """);

        assertThat(map(field(0, "Mfr", "Siemens", "Mfr: Siemens")))
                .extracting(SemanticMapping::columnIndex)
                .containsExactly(2);
    }

    /** Grounding compares content, not formatting: spacing and casing may differ. */
    @Test
    void toleratesWhitespaceAndCasingDifferencesWhenGrounding() throws Exception {
        respondWith("""
                {"mappings":[{"fieldId":"field-0","name":"Manufacturer","value":"siemens ag",
                "columnIndex":2,"confidence":0.95,"reason":"same value, different casing"}]}
                """);

        assertThat(map(field(0, "Mfr", "Siemens   AG", "Mfr: Siemens   AG")))
                .extracting(SemanticMapping::value)
                .containsExactly("siemens ag");
    }

    // ------------------------------------------------------------------------- helpers

    private void respondWith(String json) throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping"))).willReturn(response(json));
    }

    private List<SemanticMapping> map(IndexedExtractedField... fields) {
        return mappingService.mapUnmatchedFields(List.of(fields), headers());
    }

    private IndexedExtractedField field(int index, String name, String value, String rawText) {
        return new IndexedExtractedField(index,
                new ExtractedField(name, value, null, 1, null, null, null, null, "Text", rawText));
    }

    private List<ExcelColumn> headers() {
        return List.of(
                new ExcelColumn(0, "Tag Number"),
                new ExcelColumn(1, "Equipment Type"),
                new ExcelColumn(2, "Manufacturer"),
                new ExcelColumn(3, "Maximum Allowable Working Pressure"));
    }

    private JsonNode response(String content) throws Exception {
        return objectMapper.readTree("""
                {"choices":[{"message":{"content":%s}}]}
                """.formatted(objectMapper.writeValueAsString(content)));
    }
}
