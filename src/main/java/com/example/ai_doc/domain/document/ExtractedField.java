package com.example.ai_doc.domain.document;

/** A field returned by a future OCR, vision-model, or LLM implementation. */
public record ExtractedField(
        String name,
        String value,
        Double confidence,
        Integer pageNumber,
        Double x,
        Double y,
        Double width,
        Double height,
        String sourceType,
        String rawText) {

    public ExtractedField(String name,
                          String value,
                          Double confidence,
                          Integer pageNumber,
                          Double x,
                          Double y,
                          Double width,
                          Double height) {
        this(name, value, confidence, pageNumber, x, y, width, height, null, value);
    }

    public ExtractedField(String name, String value) {
        this(name, value, null, null, null, null, null, null, null, value);
    }
}
