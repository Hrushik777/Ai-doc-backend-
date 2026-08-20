package com.example.ai_doc.service.mapping;

import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.globalexception.ExternalAiServiceException;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.mapping.IndexedExtractedField;
import com.example.ai_doc.model.mapping.SemanticMapping;
import com.example.ai_doc.service.nvidia.NvidiaChatCompletionClient;
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
            nvidiaClient, objectMapper, "nvidia/nemotron-nano-9b-v2", 0.80);

    @Test
    void mapsMawpToMaximumAllowableWorkingPressure() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        {"mappings":[{"fieldIndex":0,"columnIndex":3,"confidence":0.97,
                        "reason":"MAWP is the common abbreviation"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0,
                        new ExtractedField("MAWP", "150 psi", null, 1, null, null, null, null,
                                "Text", "MAWP: 150 psi"))),
                headers());

        assertThat(mappings).containsExactly(new SemanticMapping(
                0, 3, 0.97, "MAWP is the common abbreviation"));
    }

    @Test
    void supportsSynonymMappings() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("""
                        {"mappings":[{"fieldIndex":8,"columnIndex":2,"confidence":0.91,
                        "reason":"Mfr is a common abbreviation for manufacturer"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(8,
                        new ExtractedField("Mfr", "Siemens", null, 1, null, null, null, null,
                                "Text", "Mfr: Siemens"))),
                headers());

        assertThat(mappings).containsExactly(new SemanticMapping(
                8, 2, 0.91, "Mfr is a common abbreviation for manufacturer"));
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
                        {"mappings":[{"fieldIndex":0,"columnIndex":3,"confidence":0.62,
                        "reason":"Weak evidence"}]}
                        """));

        List<SemanticMapping> mappings = mappingService.mapUnmatchedFields(
                List.of(new IndexedExtractedField(0, new ExtractedField("Pressure", "150 psi"))), headers());

        assertThat(mappings).isEmpty();
    }

    @Test
    void rejectsInvalidJsonFromTheMappingModel() throws Exception {
        given(nvidiaClient.complete(any(), eq("semantic mapping")))
                .willReturn(mappingResponse("not json"));

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
}
