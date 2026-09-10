package com.example.ai_doc.api.dto;

/**
 * The Google ID token the browser obtained from the sign-in popup, presented once in exchange
 * for a session token of ours.
 */
public record SessionRequest(String idToken) {
}
