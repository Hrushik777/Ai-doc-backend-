package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.api.error.DocumentProcessingException;
import com.example.ai_doc.api.error.ExternalAiServiceException;
import com.example.ai_doc.domain.layout.LayoutCell;
import com.example.ai_doc.domain.layout.LayoutRegion;
import com.example.ai_doc.domain.layout.LayoutRow;
import com.example.ai_doc.domain.layout.DocumentLayout;
import com.example.ai_doc.pipeline.nvidia.ModelJsonResponses;
import com.example.ai_doc.pipeline.nvidia.NvidiaChatCompletionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Names the columns for a document that arrived without a template.
 *
 * <p>This is the one place a model is genuinely required rather than merely convenient:
 * with no template there is no source of truth for what the columns should be called, so
 * naming them is a judgement about meaning, not about geometry. The layout - including each
 * element's position - is sent along, because where a value sits is often the only clue to
 * what it is.
 *
 * <p>{@link LayoutHeaderInferrer} still runs first, and its result is offered to the model
 * as a starting point. If the call fails or comes back unusable, those deterministic headers
 * are used instead, so a document without a template never fails purely because the model
 * was unreachable.
 */
@Service
public class NemotronHeaderInferenceService implements HeaderInferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NemotronHeaderInferenceService.class);

    private static final String SYSTEM_PROMPT = """
    /no_think

    You name the columns of a spreadsheet built from one document.

    You are given the document's layout: its regions, each region's structural kind, and
    every cell with the text it holds and its position on the page as x/y coordinates
    normalized to the range 0..1.

    RULES:

    - Return a flat, ordered list of column header names.
    - Name the columns the document's own data would fill, not the document's sections.
    - Prefer headers the document states about itself - a table's first row, or the labels
      in a set of labelled pairs.
    - Use the coordinates to tell a header from a value: a repeated position down a column
      means data, a single cell at the top of one means a header.
    - Where a cell holds an index and a measurement together, such as "1 - 3.5", propose
      one column for each part.
    - Do not invent columns the document has no values for.
    - Do not return a column whose only content would be blank.
    - Between 1 and 30 headers.
    - Keep each header short: a noun phrase, not a sentence.

    OUTPUT RULES:

    - Return JSON only. No markdown, no code fences, no commentary.

    Return exactly this shape:

    {"headers": ["Index", "Value"]}
    """;

    private final NvidiaChatCompletionClient nvidiaChatCompletionClient;
    private final ObjectMapper objectMapper;
    private final LayoutHeaderInferrer layoutHeaderInferrer;
    private final String mappingModel;
    private final int maxTokens;
    private final int maxCellsSent;

    public NemotronHeaderInferenceService(
            NvidiaChatCompletionClient nvidiaChatCompletionClient,
            ObjectMapper objectMapper,
            LayoutHeaderInferrer layoutHeaderInferrer,
            @Value("${nvidia.mapping.model:nvidia/nemotron-3-super-120b-a12b}") String mappingModel,
            @Value("${nvidia.headers.max-tokens:1024}") int maxTokens,
            @Value("${nvidia.headers.max-cells:400}") int maxCellsSent) {
        this.nvidiaChatCompletionClient = nvidiaChatCompletionClient;
        this.objectMapper = objectMapper;
        this.layoutHeaderInferrer = layoutHeaderInferrer;
        this.mappingModel = mappingModel;
        this.maxTokens = maxTokens;
        this.maxCellsSent = maxCellsSent;
    }

    @Override
    public List<String> inferHeaders(DocumentLayout layout) {
        List<String> deterministicHeaders = layoutHeaderInferrer.infer(layout);

        if (layout == null || layout.isEmpty()) {
            return deterministicHeaders;
        }

        try {
            List<String> proposed = parseHeaders(nvidiaChatCompletionClient.complete(
                    buildRequest(layout, deterministicHeaders), "header inference"));
            if (!proposed.isEmpty()) {
                return proposed;
            }
            LOGGER.warn("Header inference returned no usable headers; using the headers read from the layout");
        } catch (DocumentProcessingException | ExternalAiServiceException exception) {
            LOGGER.warn("Header inference failed ({}); using the headers read from the layout",
                    exception.getMessage());
        }

        return deterministicHeaders;
    }

    private ObjectNode buildRequest(DocumentLayout layout, List<String> deterministicHeaders) {
        ObjectNode document = objectMapper.createObjectNode();
        ArrayNode regions = document.putArray("regions");

        int cellBudget = maxCellsSent;

        for (LayoutRegion region : layout.regions()) {
            if (cellBudget <= 0) {
                break;
            }

            ObjectNode regionNode = regions.addObject();
            regionNode.put("page", region.page());
            regionNode.put("kind", region.kind().name());
            regionNode.put("columnCount", region.columnCount());
            regionNode.put("rowCount", region.rowCount());

            ArrayNode rows = regionNode.putArray("rows");
            for (LayoutRow row : region.rows()) {
                if (cellBudget <= 0) {
                    // A long table adds no naming information after its first rows, so the
                    // payload is capped rather than growing with the document.
                    regionNode.put("rowsTruncated", true);
                    break;
                }
                ArrayNode cells = rows.addArray();
                for (LayoutCell cell : row.cells()) {
                    cells.addObject()
                            .put("text", cell.text())
                            .put("x", round(cell.bbox().xmin()))
                            .put("y", round(cell.bbox().ymin()));
                    cellBudget--;
                }
            }
        }

        if (!deterministicHeaders.isEmpty()) {
            ArrayNode candidates = document.putArray("headersReadFromTheLayout");
            deterministicHeaders.forEach(candidates::add);
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", mappingModel);
        request.set("messages", objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode().put("role", "system").put("content", SYSTEM_PROMPT))
                .add(objectMapper.createObjectNode().put("role", "user").put("content", document.toString())));
        request.put("temperature", 0);
        request.put("max_tokens", maxTokens);
        request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        request.set("chat_template_kwargs", objectMapper.createObjectNode().put("thinking", false));
        return request;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private List<String> parseHeaders(JsonNode response) {
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new DocumentProcessingException("Header inference model returned no JSON content");
        }

        String json = ModelJsonResponses.extractJsonObject(content.asText().trim());
        if (json == null) {
            throw new DocumentProcessingException("Header inference model did not return a JSON object");
        }

        try {
            JsonNode headers = objectMapper.readTree(json).path("headers");
            if (!headers.isArray()) {
                return List.of();
            }

            // Duplicate headers would collide on the same column, so the first wins.
            List<String> names = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode header : headers) {
                String name = header.asText("").strip();
                if (!name.isEmpty() && seen.add(name.toLowerCase(java.util.Locale.ROOT))) {
                    names.add(name);
                }
            }
            return names;
        } catch (JacksonException exception) {
            throw new DocumentProcessingException("Header inference model returned invalid JSON", exception);
        }
    }
}
