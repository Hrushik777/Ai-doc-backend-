package com.example.ai_doc.model.document;

/** A field returned by a future OCR, vision-model, or LLM implementation. */
public record ExtractedField(
        String name,
        String value,
        Double confidence,
        Integer pageNumber,
        Double x,
        Double y,
        Double width,
        Double height) {

    public ExtractedField(String name, String value) {
        this(name, value, null, null, null, null, null, null);
    }
}
