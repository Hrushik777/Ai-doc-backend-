package com.example.ai_doc.service.mapping;

import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.mapping.IndexedExtractedField;
import com.example.ai_doc.model.mapping.SemanticMapping;
import com.example.ai_doc.model.mapping.SemanticMappingResponse;
import com.example.ai_doc.service.nvidia.NvidiaChatCompletionClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Uses NVIDIA's reasoning model only after deterministic header matching has failed. */
@Service
public class NemotronSemanticMappingService implements SemanticMappingService {

    private static final String SYSTEM_PROMPT = """
        /no_think

        You are a document-to-Excel mapping engine.

        The document data comes from a document parsing model and may contain
        aggregate elements such as tables, lists, sections, or text blocks.

        Your job is to identify every meaningful logical field contained in the
        supplied document data and map each logical field to the most appropriate
        Excel column.

        Rules:
        - Every meaningful logical value must be treated as a unique field.
        - Never map one aggregate document element to multiple Excel columns using
          the same field identifier.
        - If a table contains multiple key/value pairs, identify each pair
          independently.
        - Use the original value exactly as supplied.
        - Do not invent or rewrite values.
        - Use field names, raw text, document type, page number, and spatial
          information (x, y, width, height) when available.
        - Use spatial relationships such as nearby coordinates, same-row
          positions, and label/value relationships when useful.
        - Use semantic meaning, abbreviations, synonyms, and technical terminology
          to match document fields to Excel headers.
        - Only use supplied Excel column indexes.
        - Do not map one logical field to multiple Excel columns.
        - Do not map multiple logical fields to the same Excel column unless the
          fields clearly represent the same value.
        - Confidence must be between 0 and 1.
        - Return JSON only.

        Return exactly this shape:

        {
          "mappings": [
            {
              "fieldId": "string",
              "name": "string",
              "value": "string",
              "columnIndex": 0,
              "confidence": 0.0,
              "reason": "string"
            }
          ]
        }
        """;

    private final NvidiaChatCompletionClient nvidiaChatCompletionClient;
    private final ObjectMapper objectMapper;
    private final String mappingModel;
    private final double confidenceThreshold;

    public NemotronSemanticMappingService(NvidiaChatCompletionClient nvidiaChatCompletionClient,
                                          ObjectMapper objectMapper,
                                          @Value("${nvidia.mapping.model:nvidia/nemotron-nano-9b-v2}") String mappingModel,
                                          @Value("${app.mapping.llm.confidence-threshold:0.80}") double confidenceThreshold) {
        if (confidenceThreshold < 0 || confidenceThreshold > 1) {
            throw new IllegalArgumentException("app.mapping.llm.confidence-threshold must be between 0 and 1");
        }
        this.nvidiaChatCompletionClient = nvidiaChatCompletionClient;
        this.objectMapper = objectMapper;
        this.mappingModel = mappingModel;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    public List<SemanticMapping> mapUnmatchedFields(List<IndexedExtractedField> unmatchedFields,
                                                     List<ExcelColumn> headers) {
        if (unmatchedFields.isEmpty()) {
            return List.of();
        }

        JsonNode response = nvidiaChatCompletionClient.complete(
                buildRequest(unmatchedFields, headers), "semantic mapping");
        SemanticMappingResponse mappingResponse = parseMappingResponse(response);
        validateMappings(mappingResponse.mappings(), unmatchedFields, headers);

        return mappingResponse.mappings().stream()
                .filter(mapping -> mapping.confidence() >= confidenceThreshold)
                .toList();
    }

    private ObjectNode buildRequest(List<IndexedExtractedField> unmatchedFields,
                                    List<ExcelColumn> headers) {
        ObjectNode document = objectMapper.createObjectNode();
        ArrayNode documentFields = document.putArray("documentFields");
        for (IndexedExtractedField indexedField : unmatchedFields) {
            ExtractedField field = indexedField.field();
            ObjectNode documentField = documentFields.addObject();

            documentField.put(
                    "fieldId",
                    "field-" + indexedField.fieldIndex()
            );

            documentField.put(
                    "sourceFieldIndex",
                    indexedField.fieldIndex()
            );

            documentField.put(
                    "name",
                    field.name()
            );

            documentField.put(
                    "value",
                    field.value()
            );

            documentField.put(
                    "rawText",
                    field.rawText() == null ? field.value() : field.rawText()
            );

            documentField.put(
                    "type",
                    field.sourceType() == null ? "" : field.sourceType()
            );

            if (field.pageNumber() != null) {
                documentField.put("pageNumber", field.pageNumber());
            }

            if (field.x() != null) {
                documentField.put("x", field.x());
            }

            if (field.y() != null) {
                documentField.put("y", field.y());
            }

            if (field.width() != null) {
                documentField.put("width", field.width());
            }

            if (field.height() != null) {
                documentField.put("height", field.height());
            }
        }

        ArrayNode excelHeaders = document.putArray("excelHeaders");
        for (ExcelColumn header : headers) {
            excelHeaders.addObject()
                    .put("columnIndex", header.columnIndex())
                    .put("header", header.headerName());
        }

        ObjectNode systemMessage = objectMapper.createObjectNode()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT);
        ObjectNode userMessage = objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", document.toString());

        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", mappingModel);
        request.set("messages", objectMapper.createArrayNode().add(systemMessage).add(userMessage));
        request.put("temperature", 0);
        request.put("max_tokens", 2048);
        return request;
    }

    private SemanticMappingResponse parseMappingResponse(JsonNode response) {
        JsonNode content = response.path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (!content.isTextual() || content.asText().isBlank()) {
            throw new DocumentProcessingException(
                    "Semantic mapping model returned no JSON content");
        }
        System.out.println("===== MAPPING MODEL RESPONSE =====");
        System.out.println(content.asText());
        System.out.println("=================================");

        String raw = content.asText().trim();

        // Remove Markdown code fences if the model added them.
        if (raw.startsWith("```")) {
            int firstNewline = raw.indexOf('\n');
            int lastFence = raw.lastIndexOf("```");

            if (firstNewline >= 0 && lastFence > firstNewline) {
                raw = raw.substring(firstNewline + 1, lastFence).trim();
            }
        }

        // Find the actual JSON object if the model added extra text.
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new DocumentProcessingException(
                    "Semantic mapping model did not return a JSON object");
        }

        String json = raw.substring(start, end + 1);

        try {
            return objectMapper.readValue(
                    json,
                    SemanticMappingResponse.class);
        } catch (JacksonException exception) {
            throw new DocumentProcessingException(
                    "Semantic mapping model returned invalid JSON",
                    exception);
        }
    }

    private void validateMappings(
            List<SemanticMapping> mappings,
            List<IndexedExtractedField> unmatchedFields,
            List<ExcelColumn> headers) {

        Set<Integer> validColumnIndexes = headers.stream()
                .map(ExcelColumn::columnIndex)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> seenFieldIds = new java.util.HashSet<>();

        for (SemanticMapping mapping : mappings) {

            if (mapping.fieldId() == null
                    || mapping.fieldId().isBlank()
                    || mapping.value() == null
                    || mapping.value().isBlank()
                    || !validColumnIndexes.contains(mapping.columnIndex())
                    || !Double.isFinite(mapping.confidence())
                    || mapping.confidence() < 0
                    || mapping.confidence() > 1
                    || !seenFieldIds.add(mapping.fieldId())) {

                throw new DocumentProcessingException(
                        "Semantic mapping model returned an invalid mapping");
            }
        }
    }
}
