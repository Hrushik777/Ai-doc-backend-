package com.example.ai_doc.domain.layout;

import java.util.List;

/**
 * The structural reading of a whole document: every region found on every page, in
 * reading order, plus the places where side-by-side regions might really be one
 * continued list.
 *
 * <p>All coordinates here are normalized to [0,1] against their own page, so a
 * comparison between page 1 and page 3 is meaningful even when the pages differ in size.
 */
public record DocumentLayout(
        List<PageGeometry> pages,
        List<LayoutRegion> regions,
        List<ContinuationCandidate> continuations) {

    public DocumentLayout {
        pages = List.copyOf(pages);
        regions = List.copyOf(regions);
        continuations = List.copyOf(continuations);
    }

    public static DocumentLayout empty() {
        return new DocumentLayout(List.of(), List.of(), List.of());
    }

    public List<LayoutRegion> regionsOfKind(RegionKind kind) {
        return regions.stream().filter(region -> region.kind() == kind).toList();
    }

    public boolean isEmpty() {
        return regions.isEmpty();
    }
}
