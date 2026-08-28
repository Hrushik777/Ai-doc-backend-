package com.example.ai_doc.service.understanding;

import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.globalexception.UnsupportedDocumentUnderstandingException;
import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.layout.BBox;
import com.example.ai_doc.model.layout.DocumentElement;
import com.example.ai_doc.model.layout.PageGeometry;
import com.example.ai_doc.model.layout.ParsedDocument;
import com.example.ai_doc.service.nvidia.NvidiaChatCompletionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Converts image documents, and every PDF page, into positioned document elements.
 *
 * <p>Elements are emitted exactly as the parse model reported them - text, type, rectangle -
 * with no attempt to guess a label and a value. That guess used to happen here, on a single
 * colon heuristic, and it is what flattened every unlabelled layout into anonymous text: a
 * line reading {@code 1 - 3.5   7 - 4.57} carries no colon, so it lost its identity before
 * anything downstream could look at where it sat on the page. Interpretation now happens
 * after the layout is understood.
 */
@Service
public class NemotronDocumentUnderstandingService implements DocumentUnderstandingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NemotronDocumentUnderstandingService.class);
    private static final String MARKDOWN_BBOX_FUNCTION = "markdown_bbox";

    /** Coordinates no larger than this are already a fraction of the page, not pixels. */
    private static final double NORMALIZED_COORDINATE_CEILING = 1.5;

    /** Slack allowed when deciding whether reported coordinates fit the rendered raster. */
    private static final double PAGE_FIT_TOLERANCE = 1.05;

    private final NvidiaChatCompletionClient nvidiaChatCompletionClient;
    private final ObjectMapper objectMapper;
    private final PdfDocumentRenderer pdfDocumentRenderer;
    private final String parseModel;
    private final int maxTokens;
    private final ParsedDocumentFlattener parsedDocumentFlattener;

    public NemotronDocumentUnderstandingService(NvidiaChatCompletionClient nvidiaChatCompletionClient,
                                                ObjectMapper objectMapper,
                                                PdfDocumentRenderer pdfDocumentRenderer,
                                                ParsedDocumentFlattener parsedDocumentFlattener,
                                                @Value("${nvidia.parse.model:nvidia/nemotron-parse}") String parseModel,
                                                @Value("${nvidia.parse.max-tokens:4096}") int maxTokens) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("nvidia.parse.max-tokens must be greater than 0");
        }
        this.nvidiaChatCompletionClient = nvidiaChatCompletionClient;
        this.objectMapper = objectMapper;
        this.pdfDocumentRenderer = pdfDocumentRenderer;
        this.parsedDocumentFlattener = parsedDocumentFlattener;
        this.parseModel = parseModel;
        this.maxTokens = maxTokens;
    }

    @Override
    public ParsedDocument parse(MultipartFile document) {
        List<DocumentElement> elements = new ArrayList<>();
        List<PageGeometry> pages = new ArrayList<>();

        // Pages are rendered and consumed one at a time. Materializing every page bitmap up
        // front held the whole document in memory (megabytes per page at render DPI) for the
        // entire sequence of Nemotron calls; now only the in-flight page is alive.
        forEachPage(document, page -> {
            LOGGER.debug("Submitting document page {} to Nemotron Parse", page.pageNumber());
            JsonNode response = nvidiaChatCompletionClient.complete(buildRequest(page), "Nemotron Parse");

            List<DocumentElement> pageElements = parseNemotronResponse(response, page.pageNumber());
            elements.addAll(pageElements);
            pages.add(pageGeometryFor(page, pageElements));
        });

        if (elements.isEmpty()) {
            // Downstream this surfaces as "nothing matched", which points the investigation at
            // the mapping stage when the real problem is here: the parse model gave us nothing
            // to map. Enable DEBUG on this class to see the raw document elements it returned.
            LOGGER.warn("Nemotron Parse returned no document elements for {} - nothing to map",
                    document.getOriginalFilename());
        }

        return new ParsedDocument(elements, pages);
    }

    @Override
    public ExtractedDocumentData extractFields(MultipartFile document) {
        return parsedDocumentFlattener.flatten(parse(document));
    }

    /**
     * Works out what space the reported coordinates are in.
     *
     * <p>The parse model's convention is not guaranteed, so it is measured rather than
     * assumed: fractions of a page, pixels of the raster we sent, or some other fixed grid.
     * Guessing wrong would put every element in the wrong place and quietly wreck the layout
     * analysis, so the third case scales against what was actually observed.
     */
    private PageGeometry pageGeometryFor(DocumentPageImage page, List<DocumentElement> pageElements) {
        double maxX = 0;
        double maxY = 0;
        for (DocumentElement element : pageElements) {
            maxX = Math.max(maxX, element.bbox().xmax());
            maxY = Math.max(maxY, element.bbox().ymax());
        }

        if (maxX <= NORMALIZED_COORDINATE_CEILING && maxY <= NORMALIZED_COORDINATE_CEILING) {
            return new PageGeometry(page.pageNumber(), 1, 1);
        }

        if (maxX <= page.width() * PAGE_FIT_TOLERANCE && maxY <= page.height() * PAGE_FIT_TOLERANCE) {
            return new PageGeometry(page.pageNumber(), page.width(), page.height());
        }

        LOGGER.debug("Page {} coordinates exceed the rendered raster ({}x{}, observed {}x{});"
                        + " scaling against the observed extent instead",
                page.pageNumber(), page.width(), page.height(), maxX, maxY);
        return new PageGeometry(page.pageNumber(), maxX, maxY);
    }

    private void forEachPage(MultipartFile document, Consumer<DocumentPageImage> pageConsumer) {
        String contentType = document.getContentType();
        try {
            if (MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
                pdfDocumentRenderer.renderPages(document.getBytes(), pageConsumer);
                return;
            }

            if (MediaType.IMAGE_PNG_VALUE.equals(contentType) || MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
                byte[] content = document.getBytes();
                pageConsumer.accept(new DocumentPageImage(1, contentType, content,
                        imageWidth(content), imageHeight(content)));
                return;
            }
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to read document for Nemotron processing", exception);
        }

        throw new UnsupportedDocumentUnderstandingException(
                "Nemotron document understanding currently supports PDF, PNG, and JPEG documents");
    }

    private int imageWidth(byte[] content) {
        BufferedImage image = readImage(content);
        return image == null ? 0 : image.getWidth();
    }

    private int imageHeight(byte[] content) {
        BufferedImage image = readImage(content);
        return image == null ? 0 : image.getHeight();
    }

    private BufferedImage readImage(byte[] content) {
        try {
            return ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException | RuntimeException exception) {
            // An unreadable header only costs us the pixel dimensions; the coordinate-space
            // detection falls back to the observed extent, so parsing still proceeds.
            LOGGER.debug("Could not read uploaded image dimensions: {}", exception.getMessage());
            return null;
        }
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
        // A dense page - a long table rendered as LaTeX - overruns a small budget and comes
        // back truncated, which reads downstream as a page that simply had less on it.
        request.put("max_tokens", maxTokens);
        return request;
    }

    private List<DocumentElement> parseNemotronResponse(JsonNode response, int pageNumber) {
        String argumentsJson = findMarkdownBboxArguments(response);
        try {
            JsonNode argumentRoot = objectMapper.readTree(argumentsJson);
            List<DocumentElement> elements = new ArrayList<>();
            collectDocumentElements(argumentRoot, pageNumber, elements);

            if (elements.isEmpty()) {
                // The tool call came back, so the model answered - but nothing in the payload
                // looked like a document element (an object carrying bbox, text and type).
                // Log the payload so a schema change is visible instead of silent.
                LOGGER.warn("Page {} produced no document elements from the markdown_bbox payload", pageNumber);
                LOGGER.debug("markdown_bbox payload for page {}: {}", pageNumber, argumentsJson);
            }

            return elements;
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

    private void collectDocumentElements(JsonNode node, int pageNumber, List<DocumentElement> elements) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectDocumentElements(child, pageNumber, elements);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        if (isDocumentElement(node)) {
            DocumentElement element = toDocumentElement(node, pageNumber);
            if ("table".equalsIgnoreCase(element.type())) {
                elements.addAll(TableCellSplitter.split(element));
            } else {
                elements.add(element);
            }
            return;
        }

        for (var property : node.properties()) {
            collectDocumentElements(property.getValue(), pageNumber, elements);
        }
    }

    private boolean isDocumentElement(JsonNode node) {
        return node.has("bbox") && node.has("text") && node.has("type");
    }

    private DocumentElement toDocumentElement(JsonNode element, int pageNumber) {
        JsonNode bbox = element.path("bbox");
        return new DocumentElement(
                pageNumber,
                element.path("text").asText(),
                element.path("type").asText(),
                new BBox(
                        numericValue(bbox, "xmin"),
                        numericValue(bbox, "ymin"),
                        numericValue(bbox, "xmax"),
                        numericValue(bbox, "ymax")),
                element.path("confidence").isNumber() ? element.path("confidence").doubleValue() : null);
    }

    private double numericValue(JsonNode bbox, String coordinateName) {
        JsonNode coordinate = bbox.path(coordinateName);
        return coordinate.isNumber() ? coordinate.doubleValue() : 0;
    }
}
