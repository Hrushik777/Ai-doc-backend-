package com.example.ai_doc.api;

import com.example.ai_doc.auth.GoogleIdentity;
import com.example.ai_doc.auth.SessionTokenIssuer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Where the gate actually sits.
 *
 * <p>Two failures would be equally bad and neither is obvious from reading the configuration: a
 * processing endpoint left open, and the landing page accidentally closed. The product's whole
 * premise is that a stranger can see what it does before being asked for an account, so "still
 * public" is as much a requirement here as "now private".
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionTokenIssuer sessionTokenIssuer;

    // ------------------------------------------------------------ closed endpoints

    @Test
    void processingRequiresASignedInUser() throws Exception {
        for (String path : new String[]{
                "/api/documents/process",
                "/api/documents/process/explain",
                "/api/documents/process/batch"}) {

            mockMvc.perform(multipart(path).file(document()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        }
    }

    @Test
    void aTokenThisServiceDidNotSignIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/documents/process").file(document())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A valid token must actually reach the controller. Asserting "not 401" alone would pass even
     * if the request died somewhere else, so this sends a request the controller itself rejects -
     * no document part - and expects that rejection instead.
     */
    @Test
    void aValidSessionTokenReachesTheEndpoint() throws Exception {
        mockMvc.perform(multipart("/api/documents/process")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken()))
                .andExpect(status().isBadRequest());
    }

    /**
     * A rejection the browser is allowed to read.
     *
     * <p>CORS headers are added by a filter that runs before authentication, and if that order
     * ever changed the 401 would arrive without them. The browser would then discard the response
     * and hand the page an opaque network error instead - which the frontend diagnoses as a
     * sleeping backend or a bad origin. The user would be told to wait, when what they need is to
     * sign in.
     */
    @Test
    void aRejectedRequestStillCarriesCorsHeadersSoTheBrowserCanReadIt() throws Exception {
        mockMvc.perform(multipart("/api/documents/process").file(document())
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    // ------------------------------------------------------------ open endpoints

    /**
     * Preflight carries no Authorization header, by design. If it were authenticated, every
     * cross-origin call would fail before the real request was ever sent - and it would look like
     * a CORS misconfiguration rather than an authentication one.
     */
    @Test
    void preflightSucceedsWithoutCredentials() throws Exception {
        mockMvc.perform(options("/api/documents/process")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    /**
     * Reached, not blocked. A bogus Google token still fails - it is not a real one - but it must
     * fail at verification rather than at the filter chain, or nobody could ever sign in. The two
     * are told apart by the error code, since both are 401.
     */
    @Test
    void theSignInEndpointIsReachableWithoutBeingSignedIn() throws Exception {
        mockMvc.perform(post("/api/auth/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"not-a-real-google-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SIGN_IN_FAILED"));
    }

    /**
     * Anything not explicitly closed stays open, so the landing page keeps working signed out.
     *
     * <p>Asserts 404 rather than merely "not 401". An unknown route used to reach the catch-all
     * handler and come back as a 500, which makes a mistyped URL indistinguishable from the
     * server falling over - monitoring counts it as an outage and debugging starts by hunting a
     * crash that never happened.
     */
    @Test
    void anUnknownPathIsNotFoundRatherThanBlockedOrBroken() throws Exception {
        mockMvc.perform(get("/no-such-page")).andExpect(status().isNotFound());
    }

    /** A known route called with the wrong method is a 405, not a 500 and not a 401. */
    @Test
    void aKnownRouteWithTheWrongMethodSaysMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/session")).andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------- helpers

    private String validToken() {
        return sessionTokenIssuer.issue(
                new GoogleIdentity("google-subject-1", "someone@example.com", "Someone")).token();
    }

    private MockMultipartFile document() {
        return new MockMultipartFile("document", "scan.pdf", MediaType.APPLICATION_PDF_VALUE,
                com.example.ai_doc.TestFiles.pdf("1"));
    }
}
