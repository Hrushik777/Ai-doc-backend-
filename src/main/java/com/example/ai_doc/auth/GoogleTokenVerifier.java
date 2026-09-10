package com.example.ai_doc.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Verifies the Google ID token the browser obtained, once, at sign-in.
 *
 * <p>Nothing is looked up: the token carries Google's signature, and Google's public keys are
 * fetched from the JWK set and cached, so verifying is local signature arithmetic. That is the
 * whole reason this application needs no user table - a valid signature <em>is</em> the proof of
 * identity, and the claims inside it are then facts rather than assertions.
 *
 * <p>This runs only on {@code /api/auth/session}. Every later request carries our own session
 * token instead, which a different decoder validates.
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    /**
     * Google's JWK set, not its discovery document.
     *
     * <p>Deliberately not {@code issuer-uri}: discovery pins the issuer to the {@code https://}
     * form, and Google has issued ID tokens carrying the bare {@code accounts.google.com} as
     * well. Configured through discovery, those tokens are rejected - intermittently, and with an
     * error that reads like a key problem rather than an issuer mismatch. The issuer is therefore
     * checked here, accepting both spellings.
     */
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder decoder;
    private final Set<String> allowedEmails;

    public GoogleTokenVerifier(
            @Value("${app.auth.google.client-id}") String clientId,
            @Value("${app.auth.allowed-emails:}") String allowedEmails) {

        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("app.auth.google.client-id must be set");
        }

        this.allowedEmails = parseAllowedEmails(allowedEmails);

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                // Signature and expiry.
                JwtValidators.createDefault(),
                issuedByGoogle(),
                // The check that actually matters. Without it any token Google minted for any
                // other application verifies here perfectly well, because it is genuinely signed
                // by Google - so anyone could mint one from a project of their own and present it
                // as a login. Pinning the audience to our client id is what makes the signature
                // mean "issued for us".
                //
                // Spring Security's own validator rather than a hand-rolled comparison: the aud
                // claim can be a string or an array, and this is not the check to be clever with.
                new JwtAudienceValidator(clientId)));
        this.decoder = nimbusDecoder;

        if (this.allowedEmails.isEmpty()) {
            LOGGER.info("Google sign-in accepts any Google account; set app.auth.allowed-emails to restrict it");
        } else {
            LOGGER.info("Google sign-in restricted to {} allowed address(es)", this.allowedEmails.size());
        }
    }

    /**
     * @throws GoogleTokenVerificationException when the token cannot be trusted, for any reason.
     *         The caller turns this into a 401 without repeating the detail to the client: which
     *         check failed is useful in a log and useful to an attacker.
     */
    public GoogleIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new GoogleTokenVerificationException("No Google ID token was supplied");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException exception) {
            throw new GoogleTokenVerificationException("Google ID token failed verification", exception);
        }

        return toIdentity(jwt);
    }

    /**
     * Everything that happens once the signature has been accepted. Separate from {@link #verify}
     * so it can be exercised without a network round trip to Google's key set - the signature
     * check itself is Nimbus's and already proven, while the rules below are ours.
     */
    GoogleIdentity toIdentity(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (!allowedEmails.isEmpty()
                && (email == null || !allowedEmails.contains(email.toLowerCase(Locale.ROOT)))) {
            // Logged at INFO with the address: this is the expected outcome for a stranger, not a
            // fault, and knowing who was turned away is the point of having a list.
            LOGGER.info("Rejected sign-in for {} - not in app.auth.allowed-emails", email);
            throw new GoogleTokenVerificationException("This account is not permitted to use this service");
        }

        return new GoogleIdentity(jwt.getSubject(), email, jwt.getClaimAsString("name"));
    }

    static OAuth2TokenValidator<Jwt> issuedByGoogle() {
        return jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            return GOOGLE_ISSUERS.contains(issuer)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                            "invalid_issuer", "Token was not issued by Google", null));
        };
    }

    private static Set<String> parseAllowedEmails(String specification) {
        if (specification == null || specification.isBlank()) {
            return Set.of();
        }
        Set<String> emails = new LinkedHashSet<>();
        for (String entry : specification.split(",")) {
            String trimmed = entry.strip().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                emails.add(trimmed);
            }
        }
        return Set.copyOf(emails);
    }
}
