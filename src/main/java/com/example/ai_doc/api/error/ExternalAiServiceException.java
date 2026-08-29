package com.example.ai_doc.api.error;

/** Raised when NVIDIA's hosted API cannot complete a request. */
public class ExternalAiServiceException extends RuntimeException {

    public ExternalAiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalAiServiceException(String message) {
        super(message);
    }
}
