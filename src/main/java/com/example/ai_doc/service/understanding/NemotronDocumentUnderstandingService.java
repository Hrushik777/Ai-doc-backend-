package com.example.ai_doc.service.understanding;
import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.globalexception.UnsupportedDocumentUnderstandingException;
import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.service.nvidia.NvidiaChatCompletionClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/** Converts image documents, and every PDF page, into provider-neutral extracted fields. */
@Service
public class NemotronDocumentUnderstandingService implements DocumentUnderstandingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NemotronDocumentUnderstandingService.class);
    private static final String MARKDOWN_BBOX_FUNCTION = "markdown_bbox";

    private final NvidiaChatCompletionClient nvidiaChatCompletionClient;
    private final ObjectMapper objectMapper;
    private final PdfDocumentRenderer pdfDocumentRenderer;
    private final String parseModel;

    public NemotronDocumentUnderstandingService(NvidiaChatCompletionClient nvidiaChatCompletionClient,
                                                ObjectMapper objectMapper,
                                                PdfDocumentRenderer pdfDocumentRenderer,
                                                @Value("${nvidia.parse.model:nvidia/nemotron-parse}") String parseModel) {
        this.nvidiaChatCompletionClient = nvidiaChatCompletionClient;
        this.objectMapper = objectMapper;
        this.pdfDocumentRenderer = pdfDocumentRenderer;
        this.parseModel = parseModel;
    }

    @Override
    public ExtractedDocumentData extractFields(MultipartFile document) {
        List<ExtractedField> fields = new ArrayList<>();

        // Pages are rendered and consumed one at a time. Materializing every page bitmap up
        // front held the whole document in memory (megabytes per page at render DPI) for the
        // entire sequence of Nemotron calls; now only the in-flight page is alive.
        forEachPage(document, page -> {
            LOGGER.debug("Submitting document page {} to Nemotron Parse", page.pageNumber());
            JsonNode response = nvidiaChatCompletionClient.complete(
                    buildRequest(page), "Nemotron Parse");
            fields.addAll(parseNemotronResponse(response, page.pageNumber()));
        });

        if (fields.isEmpty()) {
            // Downstream this surfaces as "nothing matched", which points the investigation at
            // the mapping stage when the real problem is here: the parse model gave us nothing
            // to map. Enable DEBUG on this class to see the raw document elements it returned.
            LOGGER.warn("Nemotron Parse returned no document elements for {} - nothing to map",
                    document.getOriginalFilename());
        } else if (LOGGER.isDebugEnabled()) {
            for (int i = 0; i < fields.size(); i++) {
                ExtractedField field = fields.get(i);
                LOGGER.debug("FIELD {} | name={} | value={} | rawText={} | x={} | y={}",
                        i, field.name(), field.value(), field.rawText(), field.x(), field.y());
            }
        }

        return new ExtractedDocumentData(fields);
    }

    private void forEachPage(MultipartFile document, Consumer<DocumentPageImage> pageConsumer) {
        String contentType = document.getContentType();
        try {
            if (MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
                pdfDocumentRenderer.renderPages(document.getBytes(), pageConsumer);
                return;
            }

            if (MediaType.IMAGE_PNG_VALUE.equals(contentType) || MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
                pageConsumer.accept(new DocumentPageImage(1, contentType, document.getBytes()));
                return;
            }
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to read document for Nemotron processing", exception);
        }

        throw new UnsupportedDocumentUnderstandingException(
                "Nemotron document understanding currently supports PDF, PNG, and JPEG documents");
    }

    private ObjectNode buildRequest(DocumentPageImage page) {
        String imageDataUrl = "data:" + page.contentType() + ";base64,"
                + Base64.getEncoder().encodeToString(page.content());
        ObjectNode imageUrl = objectMapper.createObjectNode().put("url", imageDataUrl);
        ObjectNode imageContent = objectMapper.createObjectNode()
                .put("type", "image_url")
                .set("image_url", imageUrl);
        ArrayNode content = objectMapper.createArrayNode().add(imageContent);
        ObjectNode message = objectMapper.createObjectNode()
                .put("role", "user")
                .set("content", content);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", parseModel);
        request.set("messages", objectMapper.createArrayNode().add(message));
        request.put("max_tokens",  2048);
        return request;
    }

    private List<ExtractedField> parseNemotronResponse(JsonNode response, int pageNumber) {
        String argumentsJson = findMarkdownBboxArguments(response);
        try {
            JsonNode argumentRoot = objectMapper.readTree(argumentsJson);
            List<ExtractedField> fields = new ArrayList<>();
            collectDocumentElements(argumentRoot, pageNumber, fields);

            if (fields.isEmpty()) {
                // The tool call came back, so the model answered - but nothing in the payload
                // looked like a document element (an object carrying bbox, text and type).
                // Log the payload so a schema change is visible instead of silent.
                LOGGER.warn("Page {} produced no document elements from the markdown_bbox payload", pageNumber);
                LOGGER.debug("markdown_bbox payload for page {}: {}", pageNumber, argumentsJson);
            }

            return fields;
        } catch (JacksonException exception) {
            throw new DocumentProcessingException("Could not parse Nemotron document elements", exception);
        }
    }

    private String findMarkdownBboxArguments(JsonNode response) {
        JsonNode toolCalls = response.path("choices").path(0).path("message").path("tool_calls");
        if (!toolCalls.isArray()) {
            throw new DocumentProcessingException("Nemotron response did not contain tool calls");
        }

        for (JsonNode toolCall : toolCalls) {
            JsonNode function = toolCall.path("function");
            if (MARKDOWN_BBOX_FUNCTION.equals(function.path("name").asText())) {
                JsonNode arguments = function.path("arguments");
                if (arguments.isTextual() && !arguments.asText().isBlank()) {
                    return arguments.asText();
                }
            }
        }

        throw new DocumentProcessingException("Nemotron response did not contain markdown_bbox document data");
    }

    private void collectDocumentElements(JsonNode node, int pageNumber, List<ExtractedField> fields) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectDocumentElements(child, pageNumber, fields);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        if (isDocumentElement(node)) {
            ExtractedField field = toExtractedField(node, pageNumber);

            if ("table".equalsIgnoreCase(field.sourceType())) {
                fields.addAll(splitTableField(field));
            } else {
                fields.add(field);
            }
            return;
        }

        for (var property : node.properties()) {
            collectDocumentElements(
                    property.getValue(),
                    pageNumber,
                    fields
            );
        }
    }

    private boolean isDocumentElement(JsonNode node) {
        return node.has("bbox") && node.has("text") && node.has("type");
    }

    private ExtractedField toExtractedField(JsonNode element, int pageNumber) {
        String rawText = element.path("text").asText();
        String sourceType = element.path("type").asText();
        JsonNode bbox = element.path("bbox");

        Double xmin = numericValue(bbox, "xmin");
        Double ymin = numericValue(bbox, "ymin");
        Double xmax = numericValue(bbox, "xmax");
        Double ymax = numericValue(bbox, "ymax");
        Double width = xmin != null && xmax != null ? xmax - xmin : null;
        Double height = ymin != null && ymax != null ? ymax - ymin : null;
        ParsedFieldContent parsedContent = splitLabelAndValue(rawText, sourceType);

        return new ExtractedField(
                parsedContent.name(),
                parsedContent.value(),
                null,
                pageNumber,
                xmin,
                ymin,
                width,
                height,
                sourceType,
                rawText
        );
    }

    private Double numericValue(JsonNode bbox, String coordinateName) {
        JsonNode coordinate = bbox.path(coordinateName);
        return coordinate.isNumber() ? coordinate.doubleValue() : null;
    }

    private ParsedFieldContent splitLabelAndValue(String rawText, String sourceType) {
        int separatorIndex = rawText.indexOf(':');
        if (separatorIndex > 0) {
            String candidateName1 = rawText.substring(0, separatorIndex).strip();
            String candidateValue = rawText.substring(separatorIndex + 1).strip();
            if (!candidateName1.isBlank()
                    && candidateName1.length() <= 120
                    && !candidateName1.contains("\n")
                    && !candidateValue.isBlank()) {
                return new ParsedFieldContent(candidateName1, candidateValue);
            }
        }
        return new ParsedFieldContent(sourceType, rawText);
    }

    private record ParsedFieldContent(String name, String value) {
    }
    private List<ExtractedField> splitTableField(ExtractedField tableField) {

        List<ExtractedField> fields = new ArrayList<>();

        String tableText = tableField.value();

        if (tableText == null || tableText.isBlank()) {
            return List.of(tableField);
        }

        /*
         * Nemotron currently returns table content in LaTeX-like form:
         *
         * Tag Number: & P-101 \\
         * Equipment Type: & Centrifugal Pump \\
         * Mfr: & Siemens \\
         * MAWP: & 150 psi \\
         * Design Pressure: & 120 psi \\
         */
        String[] rows = tableText.split("\\\\\\\\|\\r?\\n");

        int rowIndex = 0;

        for (String row : rows) {

            String cleanedRow = row
                    .replace("\\multicolumn{2}{c}{", "")
                    .replace("\\end{tabular}", "")
                    .replace("\\begin{tabular}{cc}", "")
                    .strip();

            if (cleanedRow.isBlank()) {
                continue;
            }

            String[] parts = cleanedRow.split("&", 2);

            if (parts.length != 2) {
                continue;
            }

            String label = cleanTableText(parts[0])
                    .replaceFirst(":\\s*$", "")
                    .strip();

            String value = cleanTableText(parts[1])
                    .strip();

            if (label.isBlank() || value.isBlank()) {
                continue;
            }

            fields.add(new ExtractedField(
                    label,
                    value,
                    tableField.confidence(),
                    tableField.pageNumber(),
                    tableField.x(),
                    tableField.y(),
                    tableField.width(),
                    tableField.height(),
                    "table-cell",
                    cleanedRow
            ));

            rowIndex++;
        }

        return fields.isEmpty()
                ? List.of(tableField)
                : fields;
    }
    private String cleanTableText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\textbf{", "")
                .replace("}", "")
                .replace("**", "")
                .strip();
    }
}
