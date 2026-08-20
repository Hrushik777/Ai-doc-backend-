package com.example.ai_doc.service.nvidia;

import com.example.ai_doc.globalexception.ExternalAiServiceException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Small shared HTTP boundary for NVIDIA chat-completion calls. */
@Component
public class NvidiaChatCompletionClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NvidiaChatCompletionClient(@Qualifier("nvidiaRestClient") RestClient restClient,
                                      ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode complete(JsonNode requestBody, String operationName) {
        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new ExternalAiServiceException("NVIDIA returned an empty " + operationName + " response");
            }
            return objectMapper.readTree(responseBody);
        } catch (JacksonException | RestClientException exception) {
            exception.printStackTrace();
            throw new ExternalAiServiceException("NVIDIA " + operationName + " request failed", exception);
        }
    }
}
