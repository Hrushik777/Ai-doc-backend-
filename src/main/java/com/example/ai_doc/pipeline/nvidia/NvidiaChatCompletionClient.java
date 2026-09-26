package com.example.ai_doc.pipeline.nvidia;

import com.example.ai_doc.api.error.ExternalAiServiceException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Small shared HTTP boundary for NVIDIA chat-completion calls. */
@Component
public class NvidiaChatCompletionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NvidiaChatCompletionClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NvidiaChatCompletionClient(@Qualifier("nvidiaRestClient") RestClient restClient,
                                      ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode complete(JsonNode requestBody, String operationName) {
        try {
            // Bytes end to end. A parse request carries a whole page as base64, several MB of
            // it, and going through a String built that as UTF-16 and then encoded it again to
            // UTF-8 for the wire - two extra full copies of the page per request.
            byte[] responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(objectMapper.writeValueAsBytes(requestBody))
                    .retrieve()
                    .body(byte[].class);

            if (responseBody == null || isBlank(responseBody)) {
                throw new ExternalAiServiceException("NVIDIA returned an empty " + operationName + " response");
            }
            return objectMapper.readTree(responseBody);
        } catch (JacksonException | RestClientException exception) {
            LOGGER.warn("NVIDIA {} request failed", operationName, exception);
            throw new ExternalAiServiceException("NVIDIA " + operationName + " request failed", exception);
        }
    }

    private static boolean isBlank(byte[] body) {
        for (byte b : body) {
            if (!Character.isWhitespace(b)) {
                return false;
            }
        }
        return true;
    }
}
