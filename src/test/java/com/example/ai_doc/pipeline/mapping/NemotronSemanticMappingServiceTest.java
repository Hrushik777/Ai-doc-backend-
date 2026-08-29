package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.api.error.DocumentProcessingException;
import com.example.ai_doc.api.error.ExternalAiServiceException;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.mapping.IndexedExtractedField;
import com.example.ai_doc.domain.mapping.SemanticMapping;
import com.example.ai_doc.pipeline.nvidia.NvidiaChatCompletionClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class NemotronSemanticMappingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NvidiaChatCompletionClient nvidiaClient = mock(NvidiaChatCompletionClient.class);
    private final NemotronSemanticMappingService mappingService = new NemotronSemanticMappingService(
            nvidiaClient, objectMapper, "nvidia/nemotron-3-super-120b-a12b", 0.80, 4096, true, true);

    @Test
    void mapsMawpToMaximumAllowableWorkingPressure() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        {"mappings":[{"fieldId":"field-0","name":"MAWP","value":"150 psi",
                        "columnIndex":3,"confidence":0.97,
                        "reason":"MAWP is the common abbreviation"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0,
                        new ExtractedField("MAWP", "150 psi", null, 1, null, null, null, null,
                                "Text", "MAWP: 150 psi"))),
                headers());

        assertThat(mappings).containsExactly(new SemanticMapping(
                "field-0", "MAWP", "150 psi", 3, 0.97, "MAWP is the common abbreviation"));
    }

    @Test
    void supportsSynonymMappings() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        {"mappings":[{"fieldId":"field-8","name":"Mfr","value":"Siemens",
                        "columnIndex":2,"confidence":0.91,
                        "reason":"Mfr is a common abbreviation for manufacturer"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(8,
                        new ExtractedField("Mfr", "Siemens", null, 1, null, null, null, null,
                                "Text", "Mfr: Siemens"))),
                headers());

        assertThat(mappings).containsExactly(new SemanticMapping(
                "field-8", "Mfr", "Siemens", 2, 0.91, "Mfr is a common abbreviation for manufacturer"));
    }

    @Test
    void leavesUnrelatedFieldsUnmatched() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("{\"mappings\":[]}"));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Favourite colour", "Blue"))), headers());

        assertThat(mappings).isEmpty();
    }

    @Test
    void doesNotApplyLowConfidenceMappings() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        {"mappings":[{"fieldId":"field-0","name":"Pressure","value":"150 psi",
                        "columnIndex":3,"confidence":0.62,
                        "reason":"Weak evidence"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Pressure", "150 psi"))), headers());

        assertThat(mappings).isEmpty();
    }

    @Test
    void discardsMappingsForColumnsThatWereNotOfferedInsteadOfFailingTheDocument() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        {"mappings":[
                        {"fieldId":"field-0","name":"Mfr","value":"Siemens",
                        "columnIndex":2,"confidence":0.95,"reason":"offered column"},
                        {"fieldId":"field-1","name":"Tag","value":"P-101",
                        "columnIndex":0,"confidence":0.99,"reason":"column not offered"}]}
                        """));

        // Only column 2 is still unresolved, so only that header is offered to the model.
        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Mfr", "Siemens")),
                        new IndexedExtractedField(1, new ExtractedField("Tag", "P-101"))),
                List.of(new ExcelColumn(2, "Manufacturer")));

        assertThat(mappings).containsExactly(new SemanticMapping(
                "field-0", "Mfr", "Siemens", 2, 0.95, "offered column"));
    }

    @Test
    void readsJsonThatFollowsAReasoningTrace() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        <think>The field is labelled Mfr. Manufacturer is column {2}, so I
                        should map it there. Let me double check the other headers.</think>
                        Here is the result:
                        ```json
                        {"mappings":[{"fieldId":"field-0","name":"Mfr","value":"Siemens",
                        "columnIndex":2,"confidence":0.91,"reason":"Mfr means manufacturer"}]}
                        ```
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Mfr", "Siemens"))), headers());

        assertThat(mappings).containsExactly(new SemanticMapping(
                "field-0", "Mfr", "Siemens", 2, 0.91, "Mfr means manufacturer"));
    }

    @Test
    void reportsTruncationRatherThanBlamingTheJson() throws Exception {
        // finish_reason=length with a half-written object: the budget ran out mid-answer.
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(truncatedMappingResponse("""
                        {"mappings":[{"fieldId":"field-0","name":"Mfr","value":"Sie
                        """));

        assertThatThrownBy(() -> mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Mfr", "Siemens"))), headers()))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("ran out of output tokens");
    }

    @Test
    void retriesWithoutProviderConstraintsWhenTheEndpointRejectsThem() throws Exception {
        // Endpoints that do not accept response_format / chat_template_kwargs reject the
        // constrained first attempt; the unconstrained retry must still produce mappings.
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willThrow(new ExternalAiServiceException("NVIDIA semantic mapping request failed"))
                .willReturn(mappingResponse("""
                        {"mappings":[{"fieldId":"field-0","name":"Mfr","value":"Siemens",
                        "columnIndex":2,"confidence":0.91,"reason":"recovered on retry"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Mfr", "Siemens"))), headers());

        assertThat(mappings).containsExactly(new SemanticMapping(
                "field-0", "Mfr", "Siemens", 2, 0.91, "recovered on retry"));
    }

    @Test
    void rejectsInvalidJsonFromTheMappingModel() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("{ this is not valid json }"));

        assertThatThrownBy(() -> mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("MAWP", "150 psi"))), headers()))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("invalid JSON");
    }

    @Test
    void propagatesAControlledExceptionWhenTheLlmIsUnavailable() {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willThrow(new ExternalAiServiceException("NVIDIA semantic mapping request failed"));

        assertThatThrownBy(() -> mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("MAWP", "150 psi"))), headers()))
                .isInstanceOf(ExternalAiServiceException.class);
    }

    private List<ExcelColumn> headers() {
        return List.of(
                new ExcelColumn(0, "Tag Number"),
                new ExcelColumn(1, "Equipment Type"),
                new ExcelColumn(2, "Manufacturer"),
                new ExcelColumn(3, "Maximum Allowable Working Pressure")
        );
    }

    private JsonNode mappingResponse(String content) throws Exception {
        return objectMapper.readTree("""
                {"choices":[{"message":{"content":%s}}]}
                """.formatted(objectMapper.writeValueAsString(content)));
    }

    private JsonNode truncatedMappingResponse(String content) throws Exception {
        return objectMapper.readTree("""
                {"choices":[{"finish_reason":"length","message":{"content":%s}}]}
                """.formatted(objectMapper.writeValueAsString(content)));
    }
}
