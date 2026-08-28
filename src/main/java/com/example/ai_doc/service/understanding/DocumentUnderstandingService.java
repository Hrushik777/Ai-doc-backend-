package com.example.ai_doc.service.understanding;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.layout.ParsedDocument;
import org.springframework.web.multipart.MultipartFile;

/** Provider-neutral boundary for OCR, vision, or LLM-based document understanding. */
public interface DocumentUnderstandingService {

    /**
     * Flat name/value view of the document, for providers and callers that have no use for
     * geometry.
     */
    ExtractedDocumentData extractFields(MultipartFile document);

    /**
     * Every text element with its own rectangle, plus the page sizes those rectangles are
     * measured against - the input the layout analysis needs.
     *
     * <p>Returns {@link ParsedDocument#empty()} for a provider that cannot report positions.
     * The pipeline treats that as "no structure available" and falls back to
     * {@link #extractFields}, so a provider without geometry still works, it just cannot
     * resolve tables or multi-column layouts.
     */
    default ParsedDocument parse(MultipartFile document) {
        return ParsedDocument.empty();
    }
}
