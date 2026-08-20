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

@Component
public class PdfDocumentRenderer {

    private static final float RENDER_DPI = 150;

    public List<DocumentPageImage> renderPages(byte[] pdfContent) {
        try (PDDocument pdfDocument = Loader.loadPDF(pdfContent)) {
            if (pdfDocument.getNumberOfPages() == 0) {
                throw new DocumentProcessingException("PDF does not contain any pages");
            }

            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            List<DocumentPageImage> pages = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < pdfDocument.getNumberOfPages(); pageIndex++) {
                pages.add(renderPage(renderer, pageIndex));
            }
            return pages;
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to render PDF pages for document understanding", exception);
        }
    }

    private DocumentPageImage renderPage(PDFRenderer renderer, int pageIndex) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", outputStream)) {
                throw new DocumentProcessingException("PNG image writing is not available for PDF rendering");
            }
            return new DocumentPageImage(pageIndex + 1, "image/png", outputStream.toByteArray());
        }
    }
}
