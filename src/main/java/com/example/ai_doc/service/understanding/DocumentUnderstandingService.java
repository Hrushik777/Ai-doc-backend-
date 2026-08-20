package com.example.ai_doc.service.understanding;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import org.springframework.web.multipart.MultipartFile;

/** Provider-neutral boundary for OCR, vision, or LLM-based document understanding. */
public interface DocumentUnderstandingService {

    ExtractedDocumentData extractFields(MultipartFile document);
}
