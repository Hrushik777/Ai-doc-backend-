package com.example.ai_doc.service.validation;

import com.example.ai_doc.globalexception.EmptyFileException;
import com.example.ai_doc.globalexception.FileSizeExceededException;
import com.example.ai_doc.globalexception.InvalidFileTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileValidatorTest {

    private final DocumentFileValidator validator = new DocumentFileValidator();

    @Test
    void acceptsTheExistingSupportedDocumentTypes() {
        MockMultipartFile document = new MockMultipartFile(
                "file", "scan.png", "image/png", new byte[]{1, 2, 3});

        assertThatCode(() -> validator.validate(document)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyFilesAndUnsupportedTypes() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        MockMultipartFile textFile = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> validator.validate(emptyFile))
                .isInstanceOf(EmptyFileException.class);
        assertThatThrownBy(() -> validator.validate(textFile))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void rejectsFilesOverTheExistingTenMegabyteLimit() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                new byte[(int) DocumentFileValidator.MAX_FILE_SIZE_BYTES + 1]);

        assertThatThrownBy(() -> validator.validate(largeFile))
                .isInstanceOf(FileSizeExceededException.class);
    }
}
