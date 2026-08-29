package com.example.ai_doc.domain.mapping;

import com.example.ai_doc.domain.document.ExtractedField;

/** Keeps a stable document-field index for semantic mapping responses. */
public record IndexedExtractedField(int fieldIndex, ExtractedField field) {
}
