package com.example.ai_doc.api;

import com.example.ai_doc.TestFiles;
import com.example.ai_doc.api.error.AIServiceNotConfiguredException;
import com.example.ai_doc.api.error.GlobalExceptionHandler;
import com.example.ai_doc.domain.result.BatchItemResult;
import com.example.ai_doc.domain.result.BatchProcessedExcelFile;
import com.example.ai_doc.domain.result.ProcessedExcelFile;
import com.example.ai_doc.pipeline.document.DocumentService;
import com.example.ai_doc.pipeline.DocumentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private DocumentService documentService;
    private DocumentProcessingService documentProcessingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        documentProcessingService = mock(DocumentProcessingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DocumentController(documentService, documentProcessingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void existingUploadEndpointStillAcceptsTheFileParameter() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", TestFiles.pdf("1"));

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Document uploaded successfully"));

        verify(documentService).saveDocument(any());
    }

    @Test
    void processEndpointReturnsTheCompletedExcelAsAnAttachment() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1"));
        MockMultipartFile template = new MockMultipartFile(
                "template", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        given(documentProcessingService.process(any(), any()))
                .willReturn(new ProcessedExcelFile("completed-document.xlsx", new byte[]{4, 5, 6}));

        mockMvc.perform(multipart("/api/documents/process").file(document).file(template))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("completed-document.xlsx")))
                .andExpect(content().bytes(new byte[]{4, 5, 6}));
    }

    @Test
    void processEndpointReportsThatAiExtractionIsNotConfigured() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", TestFiles.pdf("1"));
        MockMultipartFile template = new MockMultipartFile(
                "template", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        given(documentProcessingService.process(any(), any()))
                .willThrow(new AIServiceNotConfiguredException("Document understanding is not configured"));

        mockMvc.perform(multipart("/api/documents/process").file(document).file(template))
                .andExpect(status().isNotImplemented())
                // Errors carry a stable code and never echo an internal message verbatim.
                .andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.status").value(501))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void batchProcessEndpointReturnsCompletedExcelWithBatchHeaders() throws Exception {
        MockMultipartFile document1 = new MockMultipartFile(
                "documents", "scan1.pdf", "application/pdf", TestFiles.pdf("1"));
        MockMultipartFile document2 = new MockMultipartFile(
                "documents", "scan2.pdf", "application/pdf", TestFiles.pdf("2"));
        MockMultipartFile template = new MockMultipartFile(
                "template", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        given(documentProcessingService.processBatch(any(), any()))
                .willReturn(new BatchProcessedExcelFile("completed-document.xlsx", new byte[]{4, 5, 6}, List.of(
                        new BatchItemResult("scan1.pdf", true, 1, null),
                        new BatchItemResult("scan2.pdf", false, 2, "Unsupported content type")
                )));

        mockMvc.perform(multipart("/api/documents/process/batch")
                        .file(document1).file(document2).file(template))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("completed-document.xlsx")))
                .andExpect(header().string("X-Batch-Total-Count", "2"))
                .andExpect(header().string("X-Batch-Success-Count", "1"))
                .andExpect(header().string("X-Batch-Failed-Files", "scan2.pdf"))
                .andExpect(content().bytes(new byte[]{4, 5, 6}));
    }
}
