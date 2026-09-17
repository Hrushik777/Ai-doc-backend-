package com.example.ai_doc.api;

import com.example.ai_doc.TestFiles;
import com.example.ai_doc.domain.result.ProcessedExcelFile;
import com.example.ai_doc.pipeline.DocumentProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A visitor gets a few runs before being asked for an account.
 *
 * <p>Not only a courtesy. Signing in posts to this service, so when the instance was asleep the
 * sign-in failed - and the sign-in was the only request that would have woken it. A visitor could
 * not get in, and nothing they did could fix it.
 *
 * <p>Two attempts rather than the production default, so the boundary is reached in a test that
 * stays readable. Each test invents its own client address: the allowance is a bean shared across
 * the context, so tests that all looked like the same caller would spend each other's attempts
 * and start depending on the order they ran in. Sending the address the way a proxy does exercises
 * the header path production actually uses.
 */
@SpringBootTest(properties = "app.auth.free-attempts=2")
@AutoConfigureMockMvc
class FreeAttemptAllowanceTest {

    @Autowired
    private MockMvc mockMvc;

    /** Stubbed: this is about who is let through, not about what the pipeline then does. */
    @MockitoBean
    private DocumentProcessingService documentProcessingService;

    @Test
    void letsAVisitorProcessTheAllowedNumberOfDocumentsThenAsksForAnAccount() throws Exception {
        String client = "203.0.113.10";
        given(documentProcessingService.process(any(), any()))
                .willReturn(new ProcessedExcelFile("completed-document.xlsx", new byte[]{1, 2, 3}));

        for (int attempt = 1; attempt <= 2; attempt++) {
            mockMvc.perform(multipart("/api/documents/process").file(document()).header(FORWARDED, client))
                    .andExpect(status().isOk());
        }

        // The third is where the account is asked for - and the message has to say why, since
        // "sign in" alone reads as though it had always been required.
        mockMvc.perform(multipart("/api/documents/process").file(document()).header(FORWARDED, client))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value(containsString("free runs")));
    }

    /**
     * The endpoint whose whole purpose is to be called before anything else, so the instance is
     * waking while the visitor still reads the page. It must not need an account, or it could not
     * do that job.
     */
    @Test
    void reportsServiceHealthAndRemainingAttemptsWithoutAnAccount() throws Exception {
        mockMvc.perform(get("/api/health").header(FORWARDED, "203.0.113.20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.freeAttempts").value(2))
                .andExpect(jsonPath("$.freeAttemptsRemaining").value(2));
    }

    /** Spending an attempt has to be visible to the page, or it cannot know when to prompt. */
    @Test
    void countsDownTheRemainingAttemptsAsTheyAreUsed() throws Exception {
        String client = "203.0.113.30";
        given(documentProcessingService.process(any(), any()))
                .willReturn(new ProcessedExcelFile("completed-document.xlsx", new byte[]{1}));

        mockMvc.perform(multipart("/api/documents/process").file(document()).header(FORWARDED, client))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/health").header(FORWARDED, client))
                .andExpect(jsonPath("$.freeAttemptsRemaining").value(1));
    }

    /** One visitor using up their runs must not spend anybody else's. */
    @Test
    void countsEachVisitorSeparately() throws Exception {
        given(documentProcessingService.process(any(), any()))
                .willReturn(new ProcessedExcelFile("completed-document.xlsx", new byte[]{1}));

        mockMvc.perform(multipart("/api/documents/process").file(document())
                        .header(FORWARDED, "203.0.113.40"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/health").header(FORWARDED, "203.0.113.41"))
                .andExpect(jsonPath("$.freeAttemptsRemaining").value(2));
    }

    private static final String FORWARDED = "X-Forwarded-For";

    private MockMultipartFile document() {
        return new MockMultipartFile("document", "scan.pdf", MediaType.APPLICATION_PDF_VALUE,
                TestFiles.pdf("1"));
    }
}
