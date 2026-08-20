package com.example.ai_doc.service;

import com.example.ai_doc.entity.Document;
import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.repository.DocumentRepository;
import com.example.ai_doc.service.validation.DocumentFileValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class DocumentService {

    private final Path uploadDirectory = Paths.get("Uploads");

    private final DocumentRepository documentRepository;
    private final DocumentFileValidator documentFileValidator;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentFileValidator documentFileValidator) {
        this.documentRepository = documentRepository;
        this.documentFileValidator = documentFileValidator;
    }

    public void saveDocument(MultipartFile file) {
        documentFileValidator.validate(file);

        try {
            Files.createDirectories(uploadDirectory);
            Path filePath = uploadDirectory.resolve(file.getOriginalFilename());
            Files.write(filePath, file.getBytes());
            Document document = new Document(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    filePath.toString(),
                    LocalDateTime.now()
            );
            documentRepository.save(document);

        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to save document", e);
        }
    }
}
