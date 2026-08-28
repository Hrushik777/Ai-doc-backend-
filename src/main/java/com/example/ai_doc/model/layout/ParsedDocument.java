package com.example.ai_doc.model.layout;

import java.util.List;

/**
 * Raw document-understanding output: every text element with its own rectangle, plus the
 * size of each page those rectangles are measured against.
 *
 * <p>This is the boundary between the provider and the layout analysis. Nothing here has
 * been interpreted yet - no element has been split into a name and a value, and no
 * structure has been inferred - because interpreting before the geometry is understood is
 * what previously collapsed every unlabelled layout into anonymous text.
 */
public record ParsedDocument(List<DocumentElement> elements, List<PageGeometry> pages) {

    public ParsedDocument {
        elements = List.copyOf(elements);
        pages = List.copyOf(pages);
    }

    public static ParsedDocument empty() {
        return new ParsedDocument(List.of(), List.of());
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
