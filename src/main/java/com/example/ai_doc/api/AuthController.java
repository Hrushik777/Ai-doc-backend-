package com.example.ai_doc.api;

import com.example.ai_doc.api.dto.SessionRequest;
import com.example.ai_doc.api.dto.SessionResponse;
import com.example.ai_doc.auth.GoogleIdentity;
import com.example.ai_doc.auth.GoogleTokenVerifier;
import com.example.ai_doc.auth.SessionTokenIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exchanges a Google ID token for a session token of ours.
 *
 * <p>The only place Google's token is ever accepted. It is verified once here and then discarded;
 * every request afterwards carries the token this endpoint returns, which a different decoder
 * validates. Public by necessity - a caller cannot be authenticated before signing in.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final SessionTokenIssuer sessionTokenIssuer;

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                          SessionTokenIssuer sessionTokenIssuer) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.sessionTokenIssuer = sessionTokenIssuer;
    }

    @PostMapping(value = "/session",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionResponse> createSession(@RequestBody SessionRequest request) {
        GoogleIdentity identity = googleTokenVerifier.verify(request == null ? null : request.idToken());
        SessionTokenIssuer.IssuedSession session = sessionTokenIssuer.issue(identity);

        LOGGER.info("Issued a session for {} until {}", identity.email(), session.expiresAt());

        return ResponseEntity.ok(new SessionResponse(
                session.token(), session.expiresAt(), identity.email(), identity.name()));
    }
}
