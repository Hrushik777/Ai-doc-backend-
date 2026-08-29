package com.example.ai_doc.pipeline.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The upload filename is attacker-controlled. It used to be resolved against the upload
 * directory unchanged, so a name carrying a parent reference wrote wherever it liked.
 */
class StoredFilenameTest {

    private final Path uploadDirectory = Paths.get("Uploads").toAbsolutePath().normalize();

    @ParameterizedTest
    @ValueSource(strings = {
            "../../evil.pdf",
            "..\\..\\evil.pdf",
            "/etc/passwd",
            "C:\\Windows\\System32\\evil.pdf",
            "....//....//evil.pdf",
            "sub/dir/evil.pdf",
            "..",
            ".",
            "C:evil.pdf"
    })
    void aSanitizedNameCanNeverEscapeTheUploadDirectory(String hostileName) {
        Path resolved = uploadDirectory.resolve(StoredFilename.sanitize(hostileName)).normalize();

        assertThat(resolved.startsWith(uploadDirectory))
                .as("resolved path for %s must stay inside the upload directory", hostileName)
                .isTrue();
        assertThat(resolved.getParent()).isEqualTo(uploadDirectory);
    }

    @Test
    void ordinaryNamesSurviveIntact() {
        assertThat(StoredFilename.sanitize("My_Resume.pdf")).isEqualTo("My_Resume.pdf");
        assertThat(StoredFilename.sanitize("quarterly report 2026.pdf")).isEqualTo("quarterly report 2026.pdf");
    }

    @Test
    void aNameThatSanitizesAwayEntirelyStillProducesAUsableOne() {
        assertThat(StoredFilename.sanitize("...")).isNotBlank();
        assertThat(StoredFilename.sanitize("")).isNotBlank();
        assertThat(StoredFilename.sanitize(null)).isNotBlank();
    }

    @Test
    void aVeryLongNameIsTruncatedRatherThanRejected() {
        assertThat(StoredFilename.sanitize("a".repeat(500))).hasSizeLessThanOrEqualTo(120);
    }
}
