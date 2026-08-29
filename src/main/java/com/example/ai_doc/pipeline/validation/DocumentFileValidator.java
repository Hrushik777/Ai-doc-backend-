package com.example.ai_doc.pipeline.validation;

import com.example.ai_doc.api.error.EmptyFileException;
import com.example.ai_doc.api.error.FileSizeExceededException;
import com.example.ai_doc.api.error.InvalidFileTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/** Shared validation for files accepted as source documents. */
@Component
public class DocumentFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    /**
     * Only what the understanding stage can actually read.
     *
     * <p>DOCX used to be accepted here and then rejected deep in the pipeline, after the
     * upload had been paid for, as a 422. A format this pipeline cannot process should fail
     * at the door as a 400.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    /**
     * Leading bytes that identify each accepted format.
     *
     * <p>The declared content type is supplied by the client and can say anything. Checking
     * the file's own header means an executable renamed to {@code .pdf} is rejected before
     * it reaches a parser, rather than being handed to one on the strength of a header the
     * uploader chose.
     */
    private static final List<byte[]> ACCEPTED_SIGNATURES = List.of(
            new byte[]{0x25, 0x50, 0x44, 0x46},                                     // %PDF
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},      // PNG
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}                       // JPEG
    );

    private static final int SIGNATURE_BYTES = 8;

    private final int maxBatchSize;

    public DocumentFileValidator() {
        this(20);
    }

    @Autowired
    public DocumentFileValidator(@Value("${app.batch.max-documents:20}") int maxBatchSize) {
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("app.batch.max-documents must be greater than 0");
        }
        this.maxBatchSize = maxBatchSize;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("File cannot be empty");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidFileTypeException("File type is not supported");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileSizeExceededException("File size cannot exceed 10 MB");
        }

        if (!hasAcceptedSignature(file)) {
            throw new InvalidFileTypeException(
                    "File contents do not match a supported PDF, PNG, or JPEG document");
        }
    }

    /**
     * Validates a whole batch before any of it is processed, so an oversized request is
     * refused outright instead of being discovered part-way through - by which point some
     * documents have already been paid for at the parse model.
     */
    public void validateBatch(List<MultipartFile> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new EmptyFileException("At least one document must be provided");
        }

        if (documents.size() > maxBatchSize) {
            throw new FileSizeExceededException(
                    "A batch cannot contain more than " + maxBatchSize + " documents");
        }

        long totalBytes = 0;
        for (MultipartFile document : documents) {
            totalBytes += document == null ? 0 : document.getSize();
        }
        if (totalBytes > MAX_FILE_SIZE_BYTES * maxBatchSize) {
            throw new FileSizeExceededException("The batch exceeds the total upload limit");
        }
    }

    private boolean hasAcceptedSignature(MultipartFile file) {
        byte[] header = new byte[SIGNATURE_BYTES];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.readNBytes(header, 0, SIGNATURE_BYTES);
        } catch (IOException exception) {
            return false;
        }

        for (byte[] signature : ACCEPTED_SIGNATURES) {
            if (startsWith(header, read, signature)) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWith(byte[] header, int headerLength, byte[] signature) {
        if (headerLength < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
