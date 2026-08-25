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

        The supplied document data comes from a document parsing model.
        A single supplied document element may contain multiple logical values,
        for example a table, list, or structured text block.

        Your job is to identify each meaningful logical field independently
        and map it to the appropriate Excel column.

        Rules:
        - Every logical field must have a unique fieldId.
        - If multiple logical fields come from the same source element,
          create different fieldIds such as field-2-1, field-2-2, field-2-3.
        - Never use the source element's fieldId for every logical value.
        - Extract the actual value for each logical field.
        - Preserve the original value exactly.
        - Do not invent, rewrite, summarize, or normalize values.
        - Use name, value, rawText, type, pageNumber, x, y, width and height
          when available.
        - Use spatial relationships when useful.
        - Use semantic meaning, abbreviations, synonyms and technical
          terminology when matching fields to Excel headers.
        - Only use supplied Excel column indexes.
        - Do not map titles, explanatory text, or unrelated document content
          unless there is clear evidence that they belong to an Excel header.
        - If there is no strong match, omit the mapping.
        - One logical field must not be mapped to multiple Excel columns.
        - Different logical fields may map to different Excel columns even when
          they came from the same source document element.
        - Confidence must be between 0 and 1.
        - Return JSON only.

        Return exactly this shape:
        {
          "mappings": [
            {
              "fieldId": "field-2-1",
              "name": "Tag Number",
              "value": "P-101",
              "columnIndex": 0,
              "confidence": 0.98,
              "reason": "..."
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
                    "sourceFieldId",
                    "field-" + indexedField.fieldIndex()
            );

            documentField.put(
                    "sourceFieldIndex",
                    indexedField.fieldIndex()
            );

            if (field.name() != null) {
                documentField.put("name", field.name());
            }

            if (field.value() != null) {
                documentField.put("value", field.value());
            }

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

        Set<Integer> validFieldIndexes = unmatchedFields.stream()
                .map(IndexedExtractedField::fieldIndex)
                .collect(java.util.stream.Collectors.toSet());

        Set<Integer> validColumnIndexes = headers.stream()
                .map(ExcelColumn::columnIndex)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> seenFieldIds = new java.util.HashSet<>();

        for (SemanticMapping mapping : mappings) {

            if (mapping.fieldId() == null
                    || mapping.fieldId().isBlank()
                    || mapping.name() == null
                    || mapping.name().isBlank()
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
