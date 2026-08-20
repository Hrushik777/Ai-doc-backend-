package com.example.ai_doc.controller;

import com.example.ai_doc.globalexception.AIServiceNotConfiguredException;
import com.example.ai_doc.globalexception.GlobalExceptionHandler;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.service.DocumentService;
import com.example.ai_doc.service.processing.DocumentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                "file", "scan.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Document uploaded successfully"));

        verify(documentService).saveDocument(any());
    }

    @Test
    void processEndpointReturnsTheCompletedExcelAsAnAttachment() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document", "scan.pdf", "application/pdf", new byte[]{1});
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
                "document", "scan.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile template = new MockMultipartFile(
                "template", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        given(documentProcessingService.process(any(), any()))
                .willThrow(new AIServiceNotConfiguredException("Document understanding is not configured"));

        mockMvc.perform(multipart("/api/documents/process").file(document).file(template))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("Document understanding is not configured"));
    }
}
