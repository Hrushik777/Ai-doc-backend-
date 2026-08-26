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

    GENERAL RULES:

    - Every logical field must have a unique fieldId.
    - If multiple logical fields come from the same source element,
      create different fieldIds such as field-2-1, field-2-2, field-2-3.
    - Never use the same fieldId for multiple logical values.
    - Extract the actual value for each logical field.
    - Preserve the original value exactly.
    - Do not invent, rewrite, summarize, normalize, or fabricate values.
    - Do not infer missing values from general knowledge.
    - Use name, value, rawText, type, pageNumber, x, y, width and height
      when available.
    - Use spatial relationships when useful.
    - Use semantic meaning, abbreviations, synonyms and technical
      terminology when matching fields to Excel headers.
    - Only use supplied Excel column indexes.

    SOURCE-GROUNDING RULES:

    - Every mapping must be grounded in exactly one supplied document field.
    - The mapped value must come from that same source field.
    - The returned value must be explicitly present in that field's
      value or rawText.
    - Never copy a value from another document field.
    - Never combine values from multiple document fields to create one
      mapping.
    - Never infer a value because another field contains it.
    - Never use document-wide context to manufacture or guess a value.
    - The fieldId in the mapping must identify the exact supplied source
      field from which the value was obtained.
    - If a value is not explicitly present in the selected source field,
      omit the mapping.

    STRUCTURE AND EXTRACTION RULES:

    - A single structured source element may contain multiple logical fields.
    - Extract each logical field independently when the source structure
      clearly indicates separate labeled values.
    - For tables, lists, key-value pairs, and clearly structured content,
      preserve each logical field separately.
    - Each logical field extracted from the same source element must have
      its own unique fieldId.
    - Do not assign one source element's complete value to every Excel
      column.
    - Do not create logical fields merely because words appear inside prose.
    - Do not split a generic Text, Caption, Title, Section-header, or
      explanatory paragraph into multiple fields unless the source itself
      clearly provides a labeled value structure.

    PROSE AND CONTEXT RULES:

    - The presence of an Excel header, abbreviation, synonym, or label
      inside a sentence does not make that word a document value.
    - Do not treat mentions of labels such as "Mfr", "MAWP", "Manufacturer",
      or "Tag Number" inside explanatory prose as extracted data values.
    - Do not map an Excel header merely because its name or abbreviation
      appears somewhere in rawText.
    - Do not extract words from explanatory text and treat them as values.
    - Do not infer a missing value from surrounding text.
    - Do not map titles, captions, headings, descriptions, explanatory
      paragraphs, or unrelated document content unless that same source
      field clearly contains an actual labeled value that belongs to an
      Excel header.
    - Prefer no mapping over an uncertain, inferred, or context-only mapping.

    MATCHING RULES:

    - A field may match an Excel header using exact meaning, abbreviation,
      synonym, or technical terminology.
    - Semantic matching is allowed for the relationship between the field
      name and Excel header.
    - The value itself must still come from the selected source field.
    - For example:
        Mfr: Siemens
      may map to:
        Manufacturer -> Siemens

    - But:
        "This document tests Mfr and MAWP abbreviations"
      must NOT create:
        Manufacturer -> Mfr
        Maximum Allowable Working Pressure -> MAWP

    - Never map a generic mention of a header or abbreviation as if it were
      the actual value for that header.

    - If there is no strong, directly supported match, omit the mapping.

    RESTRICTIONS:

    - One logical field must not be mapped to multiple Excel columns.
    - Different logical fields may map to different Excel columns even when
      they came from the same source document element.
    - Do not create mappings for columns that were not supplied.
    - Do not invent Excel column indexes.
    - Confidence must be between 0 and 1.

    OUTPUT RULES:

    - Return JSON only.
    - Do not return Markdown.
    - Do not wrap the JSON in code fences.
    - Do not add explanations outside the JSON object.
    - If no valid mappings exist, return an empty mappings array.
    - Every returned mapping must satisfy all source-grounding rules.

    Return exactly this shape:

    {
      "mappings": [
        {
          "fieldId": "field-2-1",
          "name": "Tag Number",
          "value": "P-101",
          "columnIndex": 0,
          "confidence": 0.98,
          "reason": "The source field explicitly contains the labeled value P-101."
        }
      ]
    }

    If no valid mappings exist, return:

    {
      "mappings": []
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
