package com.example.ai_doc.pipeline.document;

import com.example.ai_doc.persistence.Document;
import com.example.ai_doc.api.error.DocumentProcessingException;
import com.example.ai_doc.persistence.DocumentRepository;
import com.example.ai_doc.pipeline.validation.DocumentFileValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

// Disabled along with the database: without JPA auto-configuration there is no
// DocumentRepository bean for this service to depend on. The class is kept intact so
// persistence can be restored by uncommenting this annotation, the datasource and JPA
// properties, and the upload endpoint in DocumentController.
// @Service
public class DocumentService {

    private final Path uploadDirectory;
    private final DocumentRepository documentRepository;
    private final DocumentFileValidator documentFileValidator;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentFileValidator documentFileValidator,
                           @Value("${app.upload.directory:Uploads}") String uploadDirectory) {
        this.documentRepository = documentRepository;
        this.documentFileValidator = documentFileValidator;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    public void saveDocument(MultipartFile file) {
        documentFileValidator.validate(file);

        try {
            Files.createDirectories(uploadDirectory);

            Path filePath = uploadDirectory
                    .resolve(StoredFilename.sanitize(file.getOriginalFilename()))
                    .normalize();

            // Belt and braces. Sanitizing already removes every separator, so this can only
            // fire if that ever regresses - and a write outside the upload directory is not
            // a failure worth discovering in production.
            if (!filePath.startsWith(uploadDirectory)) {
                throw new DocumentProcessingException("Resolved upload path escaped the upload directory");
            }

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
