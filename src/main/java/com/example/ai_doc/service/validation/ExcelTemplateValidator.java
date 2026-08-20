package com.example.ai_doc.service.validation;

import com.example.ai_doc.globalexception.FileSizeExceededException;
import com.example.ai_doc.globalexception.InvalidExcelTemplateException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
    }
}
