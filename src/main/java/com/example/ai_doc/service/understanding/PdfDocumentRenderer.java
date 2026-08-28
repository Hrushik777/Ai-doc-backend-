package com.example.ai_doc.service.understanding;

import com.example.ai_doc.globalexception.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PdfDocumentRenderer {

    private static final float RENDER_DPI = 150;

    public List<DocumentPageImage> renderPages(byte[] pdfContent) {
        List<DocumentPageImage> pages = new ArrayList<>();
        renderPages(pdfContent, pages::add);
        return pages;
    }

    /**
     * Renders each page in turn and hands it straight to {@code pageConsumer}, so a caller
     * that processes pages sequentially never holds more than one page bitmap at a time.
     */
    public void renderPages(byte[] pdfContent, Consumer<DocumentPageImage> pageConsumer) {
        try (PDDocument pdfDocument = Loader.loadPDF(pdfContent)) {
            int pageCount = pdfDocument.getNumberOfPages();
            if (pageCount == 0) {
                throw new DocumentProcessingException("PDF does not contain any pages");
            }

            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                pageConsumer.accept(renderPage(renderer, pageIndex));
            }
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to render PDF pages for document understanding", exception);
        }
    }

    private DocumentPageImage renderPage(PDFRenderer renderer, int pageIndex) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB);

        // Size the buffer from the rendered bitmap instead of letting it start at 32 bytes and
        // double its way up, which reallocated and copied the whole page image on every growth.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(estimatedPngSize(image));
        if (!ImageIO.write(image, "png", outputStream)) {
            throw new DocumentProcessingException("PNG image writing is not available for PDF rendering");
        }
        return new DocumentPageImage(pageIndex + 1, "image/png", outputStream.toByteArray());
    }

    private int estimatedPngSize(BufferedImage image) {
        // Rendered document pages are mostly white, so PNG compresses hard. An eighth of the
        // raw RGB size is a generous starting point that still avoids repeated growth.
        long rawSize = (long) image.getWidth() * image.getHeight() * 3L;
        long estimate = Math.max(rawSize / 8L, 64L * 1024L);
        return (int) Math.min(estimate, 16L * 1024L * 1024L);
    }
}
