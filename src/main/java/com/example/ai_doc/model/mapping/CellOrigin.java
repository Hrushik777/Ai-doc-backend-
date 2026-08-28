package com.example.ai_doc.model.mapping;

import com.example.ai_doc.model.layout.BBox;

/**
 * The place on the page a structurally mapped value was read from.
 *
 * <p>Values resolved from the layout have no entry in the flat extracted-field list - they
 * come from a cell in a detected region, not from a named field - so their provenance has
 * to travel with them. Without this the explanation view goes blank for exactly the
 * documents the layout analysis exists to handle.
 */
public record CellOrigin(int page, BBox bbox, String reason) {
}
