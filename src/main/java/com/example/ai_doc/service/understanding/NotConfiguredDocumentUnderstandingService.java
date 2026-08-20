package com.example.ai_doc.service.understanding;

import com.example.ai_doc.globalexception.AIServiceNotConfiguredException;
import com.example.ai_doc.model.document.ExtractedDocumentData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Intentional temporary implementation. It prevents the API from claiming that
 * a document has been understood before an OCR or AI provider is configured.
 */
@Service
public class NotConfiguredDocumentUnderstandingService implements DocumentUnderstandingService {

    @Override
    public ExtractedDocumentData extractFields(MultipartFile document) {
        throw new AIServiceNotConfiguredException(
                "Document understanding is not configured. Configure an OCR or AI provider before processing documents.");
    }
}
