package com.example.ai_doc.pipeline.validation;

import com.example.ai_doc.TestFiles;
import com.example.ai_doc.api.error.EmptyFileException;
import com.example.ai_doc.api.error.FileSizeExceededException;
import com.example.ai_doc.api.error.InvalidFileTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileValidatorTest {

    private final DocumentFileValidator validator = new DocumentFileValidator();

    @Test
    void acceptsTheExistingSupportedDocumentTypes() {
        assertThatCode(() -> validator.validate(new MockMultipartFile(
                "file", "scan.png", "image/png", TestFiles.png()))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", TestFiles.pdf()))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(new MockMultipartFile(
                "file", "scan.jpg", "image/jpeg", TestFiles.jpeg()))).doesNotThrowAnyException();
    }

    /**
     * The declared content type is chosen by the uploader, so it cannot be the only check.
     * A file claiming to be a PDF whose bytes say otherwise is refused at the door rather
     * than being handed to a parser.
     */
    @Test
    void rejectsAFileWhoseContentsDoNotMatchItsDeclaredType() {
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "payload.pdf", "application/pdf", "MZ not a pdf".getBytes());

        assertThatThrownBy(() -> validator.validate(disguised))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    /**
     * DOCX used to pass validation and then fail deep in the pipeline as a 422, after the
     * upload had been paid for. The understanding stage reads PDF, PNG and JPEG only.
     */
    @Test
    void rejectsFormatsThePipelineCannotRead() {
        MockMultipartFile docx = new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{0x50, 0x4B, 0x03, 0x04});

        assertThatThrownBy(() -> validator.validate(docx))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void rejectsABatchLargerThanTheConfiguredLimit() {
        MockMultipartFile document = new MockMultipartFile(
                "documents", "scan.pdf", "application/pdf", TestFiles.pdf());
        DocumentFileValidator limited = new DocumentFileValidator(2);

        assertThatCode(() -> limited.validateBatch(java.util.List.of(document, document)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> limited.validateBatch(java.util.List.of(document, document, document)))
                .isInstanceOf(FileSizeExceededException.class);
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
