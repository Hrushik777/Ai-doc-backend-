package com.example.ai_doc.pipeline.validation;

import com.example.ai_doc.api.error.EmptyFileException;
import com.example.ai_doc.api.error.FileSizeExceededException;
import com.example.ai_doc.api.error.InvalidFileTypeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/** Shared validation for files accepted as source documents. */
@Component
public class DocumentFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png"
    );

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
    }
}
