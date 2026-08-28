package com.example.ai_doc.model.layout;

/**
 * One text element exactly as the parse model reported it: its own text, its own type,
 * its own rectangle.
 *
 * <p>Deliberately <em>not</em> split into a name and a value. Splitting at parse time is
 * what previously destroyed the structure of anything that was not a "Label: value" line,
 * because a element with no colon lost its identity and became an anonymous blob. The
 * split now happens during mapping, where the surrounding layout is known.
 */
public record DocumentElement(
        int page,
        String text,
        String type,
        BBox bbox,
        Double confidence) {

    public DocumentElement(int page, String text, String type, BBox bbox) {
        this(page, text, type, bbox, null);
    }

    public String textOrEmpty() {
        return text == null ? "" : text;
    }
}
