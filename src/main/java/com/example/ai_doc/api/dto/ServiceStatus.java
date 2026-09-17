package com.example.ai_doc.api.dto;

/**
 * What the browser needs to know before anyone presses anything: that the service is awake, and
 * whether this visitor still has runs left before an account is needed.
 *
 * <p>{@code freeAttemptsRemaining} lets the page decide when to ask for a sign-in. Without it the
 * only way to find out is to upload a document and be refused, which wastes the upload and puts
 * the request for an account at the least welcome moment.
 */
public record ServiceStatus(String status, int freeAttempts, int freeAttemptsRemaining) {
}
