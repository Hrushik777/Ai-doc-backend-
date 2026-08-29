package com.example.ai_doc.domain.layout;

/**
 * An axis-aligned rectangle in page space, with y increasing downward - the convention
 * Nemotron Parse and the rendered page raster both use.
 *
 * <p>Carries no unit of its own: it holds whatever space the caller put in it, and
 * {@link #normalizedBy} converts to the [0,1] space the layout analysis works in so that
 * pages of different sizes stay comparable.
 */
public record BBox(double xmin, double ymin, double xmax, double ymax) {

    public BBox {
        // Parsers occasionally emit inverted rectangles; normalising here means every
        // consumer can assume xmax >= xmin without repeating the check.
        if (xmax < xmin) {
            double swap = xmin;
            xmin = xmax;
            xmax = swap;
        }
        if (ymax < ymin) {
            double swap = ymin;
            ymin = ymax;
            ymax = swap;
        }
    }

    public double width() {
        return xmax - xmin;
    }

    public double height() {
        return ymax - ymin;
    }

    public double xCenter() {
        return (xmin + xmax) / 2.0;
    }

    public double yCenter() {
        return (ymin + ymax) / 2.0;
    }

    /**
     * Fraction of the <em>shorter</em> box's height that the two boxes share vertically.
     *
     * <p>Dividing by the shorter height rather than by either box's own height is what makes
     * this useful for row banding: a short element sitting inside a tall element's vertical
     * span scores 1.0, because they are visually on the same line.
     */
    public double verticalOverlapRatio(BBox other) {
        double overlap = Math.min(ymax, other.ymax) - Math.max(ymin, other.ymin);
        if (overlap <= 0) {
            return 0;
        }
        double shorter = Math.min(height(), other.height());
        return shorter <= 0 ? 0 : Math.min(1.0, overlap / shorter);
    }

    /** Horizontal counterpart of {@link #verticalOverlapRatio}, used for column alignment. */
    public double horizontalOverlapRatio(BBox other) {
        double overlap = Math.min(xmax, other.xmax) - Math.max(xmin, other.xmin);
        if (overlap <= 0) {
            return 0;
        }
        double narrower = Math.min(width(), other.width());
        return narrower <= 0 ? 0 : Math.min(1.0, overlap / narrower);
    }

    public BBox union(BBox other) {
        return new BBox(
                Math.min(xmin, other.xmin),
                Math.min(ymin, other.ymin),
                Math.max(xmax, other.xmax),
                Math.max(ymax, other.ymax));
    }

    /**
     * Scales this rectangle into [0,1] against a page of the given size. A non-positive
     * page dimension leaves that axis untouched, so an element whose page geometry was
     * never reported degrades to raw coordinates instead of being divided by zero.
     */
    public BBox normalizedBy(double pageWidth, double pageHeight) {
        double sx = pageWidth > 0 ? pageWidth : 1.0;
        double sy = pageHeight > 0 ? pageHeight : 1.0;
        return new BBox(xmin / sx, ymin / sy, xmax / sx, ymax / sy);
    }
}
