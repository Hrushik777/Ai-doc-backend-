package com.example.ai_doc.pipeline.understanding;

import com.example.ai_doc.api.error.ExternalAiServiceException;
import com.example.ai_doc.domain.layout.DocumentElement;
import com.example.ai_doc.domain.layout.ParsedDocument;
import com.example.ai_doc.pipeline.nvidia.NvidiaChatCompletionClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Pages are parsed concurrently, which is only safe if the result is indistinguishable from
 * parsing them one at a time.
 *
 * <p>Downstream stages read elements positionally - layout analysis bands rows by where they
 * sit, and the record mapper carries a table's header across the pages that follow it - so a
 * document whose pages arrived in completion order rather than page order would not fail
 * loudly. It would produce a plausible, quietly wrong workbook, which is why these assertions
 * are about ordering rather than about speed.
 */
class NemotronParseConcurrencyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NvidiaChatCompletionClient nvidiaClient = mock(NvidiaChatCompletionClient.class);

    /**
     * The first page is made the slowest and the last the fastest, so completion order is the
     * exact reverse of page order. A collector that appended results as they arrived would
     * pass with equal delays and fail here.
     */
    @Test
    void returnsPagesInPageOrderEvenWhenLaterPagesFinishFirst() {
        int pageCount = 6;
        given(nvidiaClient.complete(any(), eq("Nemotron Parse")))
                .willAnswer(invocation -> {
                    int page = pageOf(invocation.getArgument(0));
                    Thread.sleep((pageCount - page) * 40L);
                    return responseForPage(page);
                });

        ParsedDocument parsed = parse(4, pageCount);

        assertThat(parsed.elements()).extracting(DocumentElement::text)
                .containsExactly("page-1", "page-2", "page-3", "page-4", "page-5", "page-6");
        assertThat(parsed.elements()).extracting(DocumentElement::page)
                .containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(parsed.pages()).hasSize(pageCount);
    }

    /** Concurrency is an implementation detail: the output must match the sequential path. */
    @Test
    void producesTheSameResultAsSequentialParsing() {
        given(nvidiaClient.complete(any(), eq("Nemotron Parse")))
                .willAnswer(invocation -> responseForPage(pageOf(invocation.getArgument(0))));

        ParsedDocument concurrent = parse(4, 5);
        ParsedDocument sequential = parse(1, 5);

        assertThat(concurrent.elements().stream().map(DocumentElement::text).toList())
                .isEqualTo(sequential.elements().stream().map(DocumentElement::text).toList());
        assertThat(concurrent.elements().stream().map(DocumentElement::page).toList())
                .isEqualTo(sequential.elements().stream().map(DocumentElement::page).toList());
    }

    /** No more pages may be in flight than configured - that bound is what caps memory. */
    @Test
    void neverExceedsTheConfiguredNumberOfPagesInFlight() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        given(nvidiaClient.complete(any(), eq("Nemotron Parse")))
                .willAnswer(invocation -> {
                    peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                    try {
                        Thread.sleep(25);
                        return responseForPage(pageOf(invocation.getArgument(0)));
                    } finally {
                        inFlight.decrementAndGet();
                    }
                });

        parse(3, 12);

        assertThat(peak.get()).isLessThanOrEqualTo(3);
    }

    /**
     * A failure on one page must surface as itself. Wrapped in an ExecutionException it would
     * reach the handler as a generic internal error, losing the distinction between a model
     * that is unreachable and a document that could not be read.
     */
    @Test
    void propagatesTheOriginalFailureTypeFromAPageThatFailed() {
        given(nvidiaClient.complete(any(), eq("Nemotron Parse")))
                .willAnswer(invocation -> {
                    int page = pageOf(invocation.getArgument(0));
                    if (page == 3) {
                        throw new ExternalAiServiceException("NVIDIA Nemotron Parse request failed");
                    }
                    return responseForPage(page);
                });

        assertThatThrownBy(() -> parse(4, 5))
                .isInstanceOf(ExternalAiServiceException.class)
                .hasMessageContaining("Nemotron Parse");
    }

    // ------------------------------------------------------------------------- helpers

    private ParsedDocument parse(int concurrency, int pageCount) {
        NemotronDocumentUnderstandingService service = new NemotronDocumentUnderstandingService(
                nvidiaClient, objectMapper, new FakePageRenderer(pageCount),
                new ParsedDocumentFlattener(), "nvidia/nemotron-parse", 4096, concurrency);

        MultipartFile document = new MockMultipartFile(
                "document", "many-pages.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        return service.parse(document);
    }

    /**
     * Recovers which page a request was built for, by decoding the base64 image back out of
     * it. The page number is the only thing that distinguishes one stubbed call from another.
     */
    private int pageOf(JsonNode request) {
        String url = request.path("messages").path(0).path("content").path(0)
                .path("image_url").path("url").asText();
        String encoded = url.substring(url.indexOf("base64,") + "base64,".length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.US_ASCII);
        return Integer.parseInt(decoded.substring("page:".length()));
    }

    /** One text element per page, carrying the page number so ordering is observable. */
    private JsonNode responseForPage(int page) {
        ObjectNode bbox = objectMapper.createObjectNode()
                .put("xmin", 0).put("ymin", 0).put("xmax", 10).put("ymax", 5);
        ObjectNode element = objectMapper.createObjectNode()
                .put("text", "page-" + page)
                .put("type", "text");
        element.set("bbox", bbox);
        ArrayNode arguments = objectMapper.createArrayNode().add(element);

        ObjectNode function = objectMapper.createObjectNode()
                .put("name", "markdown_bbox")
                // The real endpoint returns the arguments as a JSON string, not as an object.
                .put("arguments", arguments.toString());
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.set("function", function);
        ObjectNode message = objectMapper.createObjectNode();
        message.set("tool_calls", objectMapper.createArrayNode().add(toolCall));
        ObjectNode choice = objectMapper.createObjectNode();
        choice.set("message", message);
        ObjectNode response = objectMapper.createObjectNode();
        response.set("choices", objectMapper.createArrayNode().add(choice));
        return response;
    }

    /**
     * Stands in for PdfDocumentRenderer so the test does not depend on PDFBox rasterizing a
     * real multi-page file. It renders serially on the calling thread exactly as the real one
     * does, which is what the in-flight bound is measured against.
     */
    private static final class FakePageRenderer extends PdfDocumentRenderer {

        private final int pageCount;

        private FakePageRenderer(int pageCount) {
            super(200f);
            this.pageCount = pageCount;
        }

        @Override
        public void renderPages(byte[] pdfContent, Consumer<DocumentPageImage> pageConsumer) {
            for (int page = 1; page <= pageCount; page++) {
                byte[] content = ("page:" + page).getBytes(StandardCharsets.US_ASCII);
                pageConsumer.accept(new DocumentPageImage(page, "image/png", content, 100, 100));
            }
        }
    }
}
