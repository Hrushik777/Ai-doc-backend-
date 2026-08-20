package com.example.ai_doc.service.understanding;

/** One rendered source page ready for NVIDIA Nemotron Parse image input. */
public record DocumentPageImage(int pageNumber, String contentType, byte[] content) {
}
