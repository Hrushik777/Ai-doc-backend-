package com.example.ai_doc.config;

import com.example.ai_doc.api.dto.ApiErrorResponse;
import com.example.ai_doc.auth.SessionTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Who may run a document through the pipeline.
 *
 * <p>Only the three processing endpoints are closed. Everything the landing page needs stays
 * open, because the point of the gate is to ask for an account at the moment work is about to be
 * spent - not to make a stranger sign in before they can see what the product does.
 *
 * <p>Stateless by construction: the bearer token is the whole credential, so there is no session
 * to keep and nothing to lose when the instance sleeps and restarts.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder sessionTokenDecoder,
                                                   ObjectMapper objectMapper) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // No cookie and no session, so there is no ambient credential for another site to
                // trigger: the token only travels when this application's own script attaches it.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // Preflight carries no Authorization header by design. Authenticate it and
                        // every cross-origin call fails before the real request is ever sent.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/documents/process",
                                "/api/documents/process/explain",
                                "/api/documents/process/batch").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(sessionTokenDecoder))
                        .authenticationEntryPoint(unauthenticatedHandler(objectMapper))
                        .accessDeniedHandler(forbiddenHandler(objectMapper)))
                // The same handlers again: the resource-server ones cover a rejected token, these
                // cover a request that arrived with no token at all.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthenticatedHandler(objectMapper))
                        .accessDeniedHandler(forbiddenHandler(objectMapper)))
                .build();
    }

    /**
     * Validates <em>our own</em> session token, not Google's. Google's is checked once, by
     * {@link com.example.ai_doc.auth.GoogleTokenVerifier}, and never seen again after sign-in.
     */
    @Bean
    public JwtDecoder sessionTokenDecoder(@Value("${app.auth.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                JwtValidators.createDefaultWithIssuer(SessionTokenIssuer.ISSUER)));
        return decoder;
    }

    /**
     * Security rejections must not reach the catch-all in GlobalExceptionHandler, which would
     * report them as a 500 and tell the browser the server broke rather than that it needs a
     * sign-in. Same body shape as every other error so the frontend reads them the same way.
     */
    private AuthenticationEntryPoint unauthenticatedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> write(response, objectMapper,
                HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Sign in to process documents");
    }

    private AccessDeniedHandler forbiddenHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> write(response, objectMapper,
                HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "This account is not permitted to use this service");
    }

    private void write(jakarta.servlet.http.HttpServletResponse response,
                       ObjectMapper objectMapper,
                       HttpStatus status,
                       String code,
                       String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiErrorResponse.of(status.value(), code, message)));
    }
}
