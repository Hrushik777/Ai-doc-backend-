package com.example.ai_doc.service;

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

        try {
            Files.createDirectories(uploadDirectory);

            Path filePath = uploadDirectory.resolve(file.getOriginalFilename());

            Files.write(filePath, file.getBytes());

        } catch (IOException e) {
            throw new RuntimeException("Failed to save document", e);
        }
    }
}