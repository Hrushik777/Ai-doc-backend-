package com.example.ai_doc.api.dto;

import java.time.Instant;

/**
 * The single body shape for every failed request.
 *
 * <p>Errors used to come back as a bare string, which gave clients nothing to branch on and
 * meant the wording of a message was effectively part of the contract. {@code code} is the
 * stable part; {@code message} is for a human reading the response.
 */
public record ApiErrorResponse(int status, String code, String message, Instant timestamp) {

    public static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message, Instant.now());
    }
}
