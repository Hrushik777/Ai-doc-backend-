package com.example.ai_doc.model.mapping;

import com.example.ai_doc.model.document.ExtractedField;

/** Keeps a stable document-field index for semantic mapping responses. */
public record IndexedExtractedField(int fieldIndex, ExtractedField field) {
}
