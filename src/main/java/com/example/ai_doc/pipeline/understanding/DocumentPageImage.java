package com.example.ai_doc.pipeline.understanding;

/**
 * One rendered source page ready for NVIDIA Nemotron Parse image input.
 *
 * <p>The pixel size travels with the image because the coordinates that come back are
 * meaningless without it: a bbox of 480 is halfway across an 960px page and off the edge
 * of a 320px one. Layout analysis normalizes against these dimensions.
 */
public record DocumentPageImage(int pageNumber, String contentType, byte[] content,
                                int width, int height) {
}
