package com.example.ai_doc.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The session token the browser carries after signing in.
 *
 * <p>There is no database behind it, so the signature is the only thing standing between a
 * request and the pipeline: a token that verifies is trusted entirely on that basis. These tests
 * cover the ways one can fail to deserve that trust.
 */
class SessionTokenTest {

    private static final String SECRET = "a-signing-secret-long-enough-for-hs256-abcdefgh";
    private static final GoogleIdentity IDENTITY =
            new GoogleIdentity("google-subject-1", "someone@example.com", "Someone Example");

    @Test
    void issuesATokenThatOurOwnDecoderAccepts() {
        SessionTokenIssuer.IssuedSession session = issuer(Duration.ofHours(24)).issue(IDENTITY);

        Jwt decoded = decoder(SECRET).decode(session.token());

        assertThat(decoded.getSubject()).isEqualTo("google-subject-1");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("someone@example.com");
        assertThat(decoded.getClaimAsString("name")).isEqualTo("Someone Example");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo(SessionTokenIssuer.ISSUER);
        assertThat(session.expiresAt()).isAfter(Instant.now().plus(Duration.ofHours(23)));
    }

    /**
     * The whole security model in one assertion: without the secret, a token cannot be forged.
     * Anyone who has the secret can mint an unlimited supply of valid sessions for any identity
     * they like, which is why the issuer refuses to start with a weak or absent one.
     */
    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        String forged = issuerWith("a-completely-different-secret-also-long-enough", Duration.ofHours(1))
                .issue(IDENTITY).token();

        assertThatThrownBy(() -> decoder(SECRET).decode(forged)).isInstanceOf(JwtException.class);
    }

    /**
     * Expiry is the only revocation this design has. With no database a token cannot be recalled,
     * so if this stopped being enforced a leaked session would last forever.
     *
     * <p>Minted directly rather than through the issuer: the default timestamp validator allows
     * 60 seconds of clock skew, so a token expiring "now" is still accepted and would make this
     * test pass for the wrong reason.
     */
    @Test
    void rejectsATokenThatHasExpired() {
        String expired = signedWith(SECRET, JwtClaimsSet.builder()
                .issuer(SessionTokenIssuer.ISSUER)
                .audience(List.of(SessionTokenIssuer.AUDIENCE))
                .subject("google-subject-1")
                .issuedAt(Instant.now().minus(Duration.ofHours(2)))
                .expiresAt(Instant.now().minus(Duration.ofHours(1)))
                .build());

        assertThatThrownBy(() -> decoder(SECRET).decode(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsATokenIssuedBySomethingOtherThanUs() {
        String foreign = signedWith(SECRET, JwtClaimsSet.builder()
                .issuer("https://accounts.google.com")
                .subject("google-subject-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .build());

        assertThatThrownBy(() -> decoder(SECRET).decode(foreign)).isInstanceOf(JwtException.class);
    }

    // ------------------------------------------------------------ startup safety

    /**
     * A signing key that is absent, blank or short must stop the application rather than be
     * quietly substituted. A predictable key makes every authentication check in the service
     * decorative, and a fallback is exactly the kind of default that reaches production unnoticed.
     */
    @Test
    void refusesToStartWithoutAUsableSigningSecret() {
        for (String unusable : java.util.Arrays.asList(null, "", "too-short")) {
            assertThatThrownBy(() -> issuerWith(unusable, Duration.ofHours(24)))
                    .as("secret %s should be refused", unusable)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("app.auth.jwt.secret");
        }
    }

    @Test
    void refusesANonPositiveLifetime() {
        assertThatThrownBy(() -> issuerWith(SECRET, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.auth.jwt.ttl");
    }

    // ------------------------------------------------------------------- helpers

    private SessionTokenIssuer issuer(Duration ttl) {
        return issuerWith(SECRET, ttl);
    }

    private SessionTokenIssuer issuerWith(String secret, Duration ttl) {
        return new SessionTokenIssuer(secret, ttl);
    }

    /** Mirrors SecurityConfiguration.sessionTokenDecoder, so the tests exercise the real rules. */
    private NimbusJwtDecoder decoder(String secret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(secret)).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                JwtValidators.createDefaultWithIssuer(SessionTokenIssuer.ISSUER)));
        return decoder;
    }

    private String signedWith(String secret, JwtClaimsSet claims) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
