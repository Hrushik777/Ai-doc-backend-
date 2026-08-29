package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.api.error.DocumentProcessingException;
import com.example.ai_doc.api.error.ExternalAiServiceException;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.mapping.IndexedExtractedField;
import com.example.ai_doc.domain.mapping.SemanticMapping;
import com.example.ai_doc.domain.mapping.SemanticMappingResponse;
import com.example.ai_doc.pipeline.nvidia.ModelJsonResponses;
import com.example.ai_doc.pipeline.nvidia.NvidiaChatCompletionClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Uses NVIDIA's reasoning model only after deterministic header matching has failed. */
@Service
public class NemotronSemanticMappingService implements SemanticMappingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NemotronSemanticMappingService.class);

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

    - A source element can itself BE the value for an Excel header even when
      it carries no explicit "Label: value" structure. Judge by what the
      element's content IS, not by the element type reported for it.
      For example, in a CV or profile document:
        a heading holding a person's name is the value for a "Name" header;
        a list of technologies is the value for a "Skills" header;
        a job or degree entry is the value for an "Experience" or
        "Education" header.
      Documents that use headings, lists and sections instead of labeled
      fields are normal, and must still be mapped.

    PROSE AND CONTEXT RULES:

    - The presence of an Excel header, abbreviation, synonym, or label
      inside a sentence does not make that word a document value.
    - Do not treat mentions of labels such as "Mfr", "MAWP", "Manufacturer",
      or "Tag Number" inside explanatory prose as extracted data values.
    - Do not map an Excel header merely because its name or abbreviation
      appears somewhere in rawText.
    - Do not extract words from explanatory text and treat them as values.
    - Do not infer a missing value from surrounding text.
    - Do not map an element that is merely ABOUT a header, or that only
      mentions it in passing, rather than being the header's actual value.
    - Prefer no mapping over an uncertain, inferred, or context-only mapping.

    The distinction that matters is grounding, not element type:
      map an element when its own content is what the header asks for;
      do not map an element that only talks about the header.
    So a heading reading "SHIVAM KUMAR" IS the value for "Name",
    while the sentence "This document tests Mfr and MAWP abbreviations"
    is NOT a value for "Manufacturer" or "Maximum Allowable Working
    Pressure" - it only mentions them.

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

    private static final String STRICT_JSON_REMINDER = """
    Your previous response could not be parsed as JSON. Respond again with ONLY a single
    valid JSON object matching the schema shown earlier - no explanations, no prose,
    no markdown fences, no text before or after the JSON. Every mapping entry must use
    exactly the keys fieldId, name, value, columnIndex, confidence, reason.
    """;

    private static final int MAX_MAPPING_ATTEMPTS = 2;

    private static final String TRUNCATED_RESPONSE_MESSAGE =
            "Semantic mapping model ran out of output tokens before completing its JSON object"
                    + " - raise nvidia.mapping.max-tokens, or use a mapping model that does not"
                    + " spend the budget on reasoning";

    private final NvidiaChatCompletionClient nvidiaChatCompletionClient;
    private final ObjectMapper objectMapper;
    private final String mappingModel;
    private final double confidenceThreshold;
    private final int maxTokens;
    private final boolean jsonMode;
    private final boolean disableThinking;

    public NemotronSemanticMappingService(NvidiaChatCompletionClient nvidiaChatCompletionClient,
                                          ObjectMapper objectMapper,
                                          @Value("${nvidia.mapping.model:nvidia/nemotron-3-super-120b-a12b}") String mappingModel,
                                          @Value("${app.mapping.llm.confidence-threshold:0.80}") double confidenceThreshold,
                                          @Value("${nvidia.mapping.max-tokens:4096}") int maxTokens,
                                          @Value("${nvidia.mapping.json-mode:true}") boolean jsonMode,
                                          @Value("${nvidia.mapping.disable-thinking:true}") boolean disableThinking) {
        if (confidenceThreshold < 0 || confidenceThreshold > 1) {
            throw new IllegalArgumentException("app.mapping.llm.confidence-threshold must be between 0 and 1");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("nvidia.mapping.max-tokens must be greater than 0");
        }
        this.nvidiaChatCompletionClient = nvidiaChatCompletionClient;
        this.objectMapper = objectMapper;
        this.mappingModel = mappingModel;
        this.confidenceThreshold = confidenceThreshold;
        this.maxTokens = maxTokens;
        this.jsonMode = jsonMode;
        this.disableThinking = disableThinking;
    }

    @Override
    public List<SemanticMapping> mapUnmatchedFields(List<IndexedExtractedField> unmatchedFields,
                                                     List<ExcelColumn> headers) {
        if (unmatchedFields.isEmpty()) {
            return List.of();
        }

        SemanticMappingResponse mappingResponse = null;
        RuntimeException lastFailure = null;

        // Attempt 1 asks the endpoint to constrain decoding to JSON and to switch reasoning
        // off, which is what stops prose and truncation happening at all. Attempt 2 drops both
        // constraints (in case this model or endpoint rejects them), adds a stricter reminder,
        // and doubles the token budget.
        for (int attempt = 1; attempt <= MAX_MAPPING_ATTEMPTS; attempt++) {
            boolean useProviderConstraints = attempt == 1 && (jsonMode || disableThinking);

            try {
                JsonNode response = nvidiaChatCompletionClient.complete(
                        buildRequest(unmatchedFields, headers, attempt, useProviderConstraints),
                        "semantic mapping");
                mappingResponse = parseMappingResponse(response);
                lastFailure = null;
                break;
            } catch (DocumentProcessingException | ExternalAiServiceException failure) {
                lastFailure = failure;
                if (attempt < MAX_MAPPING_ATTEMPTS) {
                    LOGGER.warn("Semantic mapping attempt {} failed ({}); retrying without provider constraints"
                                    + " and a larger token budget",
                            attempt, failure.getMessage());
                }
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }

        return validateMappings(mappingResponse.mappings(), headers).stream()
                .filter(mapping -> mapping.confidence() >= confidenceThreshold)
                .toList();
    }

    private ObjectNode buildRequest(List<IndexedExtractedField> unmatchedFields,
                                    List<ExcelColumn> headers,
                                    int attempt,
                                    boolean useProviderConstraints) {
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

            // When an element carries no "Label: value" structure, extraction falls back to
            // using the layout block type ("Text", "Section-header", "List-item") as the field
            // name. Forwarding that tells the model the field is literally *called*
            // "Section-header", so it correctly refuses to match it to a header like "Name".
            // Omit the placeholder and let the model judge from the content and type instead.
            boolean nameIsBlockTypePlaceholder =
                    field.name() != null && field.name().equals(field.sourceType());

            if (field.name() != null && !nameIsBlockTypePlaceholder) {
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

        ArrayNode messages = objectMapper.createArrayNode().add(systemMessage).add(userMessage);

        if (attempt > 1) {
            messages.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", STRICT_JSON_REMINDER));
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", mappingModel);
        request.set("messages", messages);
        request.put("temperature", 0);
        // Temperature is 0, so a retry only differs by the stricter reminder, a larger budget,
        // and dropping the provider constraints - which is what recovers a truncated response.
        request.put("max_tokens", maxTokens * attempt);

        if (useProviderConstraints) {
            if (jsonMode) {
                // Constrained decoding: the endpoint will not let the model emit anything but a
                // JSON object, which removes prose and reasoning traces at the source instead of
                // leaving us to scrape them off afterwards.
                request.set("response_format",
                        objectMapper.createObjectNode().put("type", "json_object"));
            }
            if (disableThinking) {
                // Reasoning models spend their output budget thinking before answering, which is
                // what truncates the JSON. The prompt's /no_think token only works on some
                // Nemotron builds; this is the chat-template level switch.
                request.set("chat_template_kwargs",
                        objectMapper.createObjectNode().put("thinking", false));
            }
        }

        return request;
    }

    private SemanticMappingResponse parseMappingResponse(JsonNode response) {
        JsonNode choice = response.path("choices").path(0);

        // finish_reason=length means the model hit its token ceiling mid-answer. That is the
        // usual cause of a half-written JSON object, so it gets its own actionable message
        // instead of a generic "invalid JSON".
        boolean truncated = "length".equals(choice.path("finish_reason").asText(""));

        JsonNode content = choice.path("message").path("content");

        if (!content.isTextual() || content.asText().isBlank()) {
            throw new DocumentProcessingException(truncated
                    ? TRUNCATED_RESPONSE_MESSAGE
                    : "Semantic mapping model returned no JSON content");
        }

        String raw = content.asText().trim();
        LOGGER.debug("Semantic mapping model response: {}", raw);

        String json = ModelJsonResponses.extractJsonObject(raw);

        if (json == null) {
            throw new DocumentProcessingException(truncated
                    ? TRUNCATED_RESPONSE_MESSAGE
                    : "Semantic mapping model did not return a JSON object");
        }

        try {
            return objectMapper.readValue(json, SemanticMappingResponse.class);
        } catch (JacksonException exception) {
            throw new DocumentProcessingException(truncated
                    ? TRUNCATED_RESPONSE_MESSAGE
                    : "Semantic mapping model returned invalid JSON",
                    exception);
        }
    }

    private List<SemanticMapping> validateMappings(
            List<SemanticMapping> mappings,
            List<ExcelColumn> headers) {

        Set<Integer> offeredColumnIndexes = new HashSet<>(headers.size());
        for (ExcelColumn header : headers) {
            offeredColumnIndexes.add(header.columnIndex());
        }

        Set<String> seenFieldIds = new HashSet<>();
        List<SemanticMapping> usableMappings = new ArrayList<>(mappings.size());

        for (SemanticMapping mapping : mappings) {

            // A malformed mapping means the model ignored the response contract, so the
            // document still fails loudly rather than silently losing data.
            if (mapping.fieldId() == null
                    || mapping.fieldId().isBlank()
                    || mapping.name() == null
                    || mapping.name().isBlank()
                    || mapping.value() == null
                    || mapping.value().isBlank()
                    || !Double.isFinite(mapping.confidence())
                    || mapping.confidence() < 0
                    || mapping.confidence() > 1) {

                throw new DocumentProcessingException(
                        "Semantic mapping model returned an invalid mapping");
            }

            // A repeated fieldId used to fail the whole document. It is no longer a contract
            // violation: one source element can legitimately supply values for several rows,
            // and the structural stage now produces exactly that. Only the first mapping for
            // a field can win the column anyway, so a repeat is dropped rather than fatal.
            if (!seenFieldIds.add(mapping.fieldId())) {
                LOGGER.debug("Discarding a repeated mapping for source field {}", mapping.fieldId());
                continue;
            }

            // Only unresolved columns are offered to the model. A mapping onto a column we
            // deliberately withheld is well-formed but unusable - the deterministic value for
            // that column wins regardless - so it is dropped instead of failing the document.
            if (!offeredColumnIndexes.contains(mapping.columnIndex())) {
                LOGGER.debug("Discarding semantic mapping for column {} which was not offered to the model",
                        mapping.columnIndex());
                continue;
            }

            usableMappings.add(mapping);
        }

        return usableMappings;
    }
}
