package com.example.ai_doc.pipeline.validation;

import com.example.ai_doc.api.error.FileSizeExceededException;
import com.example.ai_doc.api.error.InvalidExcelTemplateException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.util.Locale;
import java.util.Set;

/** Validates the transport-level constraints of client supplied XLSX templates. */
@Component
public class ExcelTemplateValidator {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Set<String> PERMITTED_CONTENT_TYPES = Set.of(
            XLSX_CONTENT_TYPE,
            "application/octet-stream"
    );

    public void validate(MultipartFile template) {
        if (template == null || template.isEmpty()) {
            throw new InvalidExcelTemplateException("Excel template cannot be empty");
        }

        if (template.getSize() > DocumentFileValidator.MAX_FILE_SIZE_BYTES) {
            throw new FileSizeExceededException("File size cannot exceed 10 MB");
        }

        String filename = template.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new InvalidExcelTemplateException("Excel template must be an .xlsx file");
        }

        String contentType = template.getContentType();
        if (contentType != null && !PERMITTED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidExcelTemplateException("Excel template must use the XLSX content type");
        }

        // An .xlsx is a zip archive. Checking for the archive header rejects a mislabelled
        // file here rather than letting POI fail on it with a less useful message.
        if (!isZipArchive(template)) {
            throw new InvalidExcelTemplateException("Excel template is not a readable XLSX file");
        }
    }

    private boolean isZipArchive(MultipartFile template) {
        byte[] header = new byte[4];
        try (InputStream inputStream = template.getInputStream()) {
            if (inputStream.readNBytes(header, 0, 4) < 4) {
                return false;
            }
        } catch (IOException exception) {
            return false;
        }
        return header[0] == 0x50 && header[1] == 0x4B
                && (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07);
    }
}
