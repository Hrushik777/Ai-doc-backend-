package com.example.ai_doc.model.document;

import java.util.List;

/** Structured document-understanding output independent of any AI provider. */
public record ExtractedDocumentData(List<ExtractedField> fields) {

    public ExtractedDocumentData {
        fields = List.copyOf(fields);
    }
}
