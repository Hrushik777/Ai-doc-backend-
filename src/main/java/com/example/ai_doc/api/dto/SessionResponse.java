package com.example.ai_doc.api.dto;

import java.time.Instant;

/**
 * A signed-in session.
 *
 * <p>The email and name are returned alongside the token so the browser can show who is signed in
 * without decoding it. A client that reads claims out of a token it cannot verify is reading
 * unverified input; sending them separately keeps the token opaque to the frontend, which is the
 * only party that has no way to check it.
 *
 * <p>{@code expiresAt} lets the browser renew before a request fails rather than after.
 */
public record SessionResponse(String token, Instant expiresAt, String email, String name) {
}
