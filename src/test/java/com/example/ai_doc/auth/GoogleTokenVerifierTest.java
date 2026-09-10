package com.example.ai_doc.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules applied to a Google ID token once its signature has been accepted.
 *
 * <p>The signature check itself is Nimbus's and is not re-tested here. What is tested is
 * everything a correctly-signed token can still be wrong about - and the audience case is the one
 * that matters most, because a token failing it is not malformed or forged. It is a real Google
 * token, genuinely signed, that was simply issued for somebody else's application.
 */
class GoogleTokenVerifierTest {

    private static final String OUR_CLIENT_ID = "our-app.apps.googleusercontent.com";

    // ------------------------------------------------------------------ audience

    /**
     * The check that stops anyone with a Google Cloud project of their own from minting a login.
     * If this ever silently stops running, every other check here still passes.
     */
    @Test
    void rejectsARealGoogleTokenThatWasIssuedForADifferentApplication() {
        Jwt someoneElsesToken = googleToken(claims -> claims
                .put("aud", List.of("someone-elses-app.apps.googleusercontent.com")));

        assertThat(new JwtAudienceValidator(OUR_CLIENT_ID).validate(someoneElsesToken).hasErrors())
                .isTrue();
    }

    @Test
    void acceptsATokenIssuedForUs() {
        assertThat(new JwtAudienceValidator(OUR_CLIENT_ID).validate(googleToken()).hasErrors())
                .isFalse();
    }

    /** Google may list several audiences; ours being among them is what counts. */
    @Test
    void acceptsATokenListingOurClientIdAlongsideOthers() {
        Jwt token = googleToken(claims -> claims.put("aud", List.of("another-app", OUR_CLIENT_ID)));

        assertThat(new JwtAudienceValidator(OUR_CLIENT_ID).validate(token).hasErrors()).isFalse();
    }

    // -------------------------------------------------------------------- issuer

    /**
     * Google has issued both spellings. Accepting only the {@code https://} form - which is what
     * discovery-based configuration does - rejects real tokens intermittently, with an error that
     * reads like a key problem.
     */
    @Test
    void acceptsBothSpellingsOfGooglesIssuer() {
        for (String issuer : List.of("https://accounts.google.com", "accounts.google.com")) {
            Jwt token = googleToken(claims -> claims.put("iss", issuer));
            OAuth2TokenValidatorResult result = GoogleTokenVerifier.issuedByGoogle().validate(token);

            assertThat(result.hasErrors()).as("issuer %s should be accepted", issuer).isFalse();
        }
    }

    @Test
    void rejectsATokenFromAnyOtherIssuer() {
        Jwt token = googleToken(claims -> claims.put("iss", "https://accounts.google.com.evil.test"));

        assertThat(GoogleTokenVerifier.issuedByGoogle().validate(token).hasErrors()).isTrue();
    }

    // ----------------------------------------------------------------- allowlist

    @Test
    void admitsAnyGoogleAccountWhenNoAllowlistIsConfigured() {
        GoogleIdentity identity = verifierAllowing("").toIdentity(googleToken());

        assertThat(identity.email()).isEqualTo("someone@example.com");
        assertThat(identity.subject()).isEqualTo("google-subject-1");
    }

    @Test
    void admitsAListedAddress() {
        assertThat(verifierAllowing("someone@example.com,other@example.com")
                .toIdentity(googleToken()).email())
                .isEqualTo("someone@example.com");
    }

    @Test
    void refusesAnAddressThatIsNotListed() {
        assertThatThrownBy(() -> verifierAllowing("only-me@example.com").toIdentity(googleToken()))
                .isInstanceOf(GoogleTokenVerificationException.class)
                .hasMessageContaining("not permitted");
    }

    /** An allowlist that ignored casing would be trivially bypassed by capitalising an address. */
    @Test
    void matchesAddressesWithoutRegardToCasingOrSurroundingSpace() {
        GoogleTokenVerifier verifier = verifierAllowing("  SomeOne@Example.COM , other@example.com ");

        assertThat(verifier.toIdentity(googleToken()).email()).isEqualTo("someone@example.com");
    }

    @Test
    void refusesATokenCarryingNoEmailWhenAnAllowlistIsConfigured() {
        Jwt withoutEmail = googleToken(claims -> claims.remove("email"));

        assertThatThrownBy(() -> verifierAllowing("someone@example.com").toIdentity(withoutEmail))
                .isInstanceOf(GoogleTokenVerificationException.class);
    }

    // ------------------------------------------------------------------- helpers

    private GoogleTokenVerifier verifierAllowing(String allowedEmails) {
        return new GoogleTokenVerifier(OUR_CLIENT_ID, allowedEmails);
    }

    private Jwt googleToken() {
        return googleToken(claims -> { });
    }

    /** A decoded, already-signature-checked Google ID token, with claims the caller may adjust. */
    private Jwt googleToken(java.util.function.Consumer<Map<String, Object>> customize) {
        Map<String, Object> claims = new java.util.HashMap<>(Map.of(
                "iss", "https://accounts.google.com",
                "aud", List.of(OUR_CLIENT_ID),
                "sub", "google-subject-1",
                "email", "someone@example.com",
                "name", "Someone Example"));
        customize.accept(claims);

        Jwt.Builder builder = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .issuedAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(3600));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
