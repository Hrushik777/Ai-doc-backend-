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
        List<DocumentPageImage> pages = createPageImages(document);
        List<ExtractedField> fields = new ArrayList<>();

        for (DocumentPageImage page : pages) {
            LOGGER.debug("Submitting document page {} to Nemotron Parse", page.pageNumber());
            JsonNode response = nvidiaChatCompletionClient.complete(
                    buildRequest(page), "Nemotron Parse");
            fields.addAll(parseNemotronResponse(response, page.pageNumber()));
        }

        for(int i =0; i< fields.size();i++){
            ExtractedField field = fields.get(i);

            System.out.println(
                    "FIELD " + i
                            + " | name=" + field.name()
                            + " | value=" + field.value()
                            + " | rawText=" + field.rawText()
                            + " | x=" + field.x()
                            + " | y=" + field.y()
            );
        }

        return new ExtractedDocumentData(fields);
    }

    private List<DocumentPageImage> createPageImages(MultipartFile document) {
        String contentType = document.getContentType();
        try {
            if (MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
                return pdfDocumentRenderer.renderPages(document.getBytes());
            }

            if (MediaType.IMAGE_PNG_VALUE.equals(contentType) || MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
                return List.of(new DocumentPageImage(1, contentType, document.getBytes()));
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
        request.put("max_tokens", 8192);
        return request;
    }

    private List<ExtractedField> parseNemotronResponse(JsonNode response, int pageNumber) {
        String argumentsJson = findMarkdownBboxArguments(response);
        try {
            JsonNode argumentRoot = objectMapper.readTree(argumentsJson);
            List<ExtractedField> fields = new ArrayList<>();
            collectDocumentElements(argumentRoot, pageNumber, fields);
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
            fields.add(toExtractedField(node, pageNumber));
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
}
