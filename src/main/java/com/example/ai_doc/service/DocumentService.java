package com.example.ai_doc.service;

import com.example.ai_doc.globalexception.EmptyFileException;
import com.example.ai_doc.globalexception.FileSizeExceededException;
import com.example.ai_doc.globalexception.InvalidFileTypeException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentService {

    private final Path uploadDirectory = Paths.get("Uploads");

    public void saveDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException("File cannot be empty");
        }
        String contentType = file.getContentType();

        if (!isAllowedFileType(contentType)) {
            throw new InvalidFileTypeException("File type is not supported");
        }
        long maxFileSize = 10 * 1024 * 1024; // 10 MB

        if (file.getSize() > maxFileSize) {
            throw new FileSizeExceededException("File size cannot exceed 10 MB");
        }
        try {
            Files.createDirectories(uploadDirectory);

            Path filePath = uploadDirectory.resolve(file.getOriginalFilename());

            Files.write(filePath, file.getBytes());

        } catch (IOException e) {
            throw new RuntimeException("Failed to save document", e);
        }
    }
    private boolean isAllowedFileType(String contentType) {

        return contentType != null &&
                (contentType.equals("application/pdf") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/png"));
    }
}