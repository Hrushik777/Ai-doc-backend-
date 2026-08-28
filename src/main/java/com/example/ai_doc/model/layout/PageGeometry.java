package com.example.ai_doc.model.layout;

/**
 * Size of one source page, in the same units as the element coordinates parsed from it.
 *
 * <p>Needed because layout decisions are relative - a "wide" gutter means wide compared to
 * this page - and because two pages of different sizes are only comparable once both have
 * been scaled into [0,1].
 */
public record PageGeometry(int page, double width, double height) {
}
