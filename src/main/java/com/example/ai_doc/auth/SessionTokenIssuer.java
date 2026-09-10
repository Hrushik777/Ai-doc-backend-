package com.example.ai_doc.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Mints the session token the browser uses after signing in.
 *
 * <p>Google's token is deliberately not reused for this. It lasts a fixed hour, its claims are
 * Google's to define, and depending on it directly would tie the API's notion of a session to one
 * identity provider. Exchanging it once for a token of our own means the lifetime is ours to
 * choose and a second provider becomes a change behind {@code /api/auth/session} that the browser
 * never observes.
 */
@Component
public class SessionTokenIssuer {

    /** HS256 is defined over a key of at least this length; a shorter one is not merely weak. */
    private static final int MINIMUM_SECRET_BYTES = 32;

    /** Shared with the decoder, which rejects any token not claiming to come from us. */
    public static final String ISSUER = "ai-doc";

    public static final String AUDIENCE = "ai-doc-api";

    private final JwtEncoder encoder;
    private final Duration ttl;

    public SessionTokenIssuer(@Value("${app.auth.jwt.secret}") String secret,
                              @Value("${app.auth.jwt.ttl:24h}") Duration ttl) {

        // Refused rather than defaulted. A generated or fallback signing key turns every
        // authentication check in the application into a formality, and it is exactly the kind of
        // default that is never noticed until someone else has minted a token with it. Failing at
        // startup is loud; silently signing with a guessable key is not.
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.auth.jwt.secret must be set and at least " + MINIMUM_SECRET_BYTES
                            + " bytes; generate one with: openssl rand -base64 48");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("app.auth.jwt.ttl must be a positive duration");
        }

        SecretKey key = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.ttl = ttl;
    }

    public IssuedSession issue(GoogleIdentity identity) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(java.util.List.of(AUDIENCE))
                .subject(identity.subject())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", identity.email())
                .claim("name", identity.name())
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new IssuedSession(token, expiresAt);
    }

    /** The token and when it stops working, so the browser can renew before a request fails. */
    public record IssuedSession(String token, Instant expiresAt) {
    }
}
