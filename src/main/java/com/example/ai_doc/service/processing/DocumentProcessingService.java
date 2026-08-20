package com.example.ai_doc.service.processing;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.service.excel.ExcelService;
import com.example.ai_doc.service.mapping.HeaderFieldMapper;
import com.example.ai_doc.service.understanding.DocumentUnderstandingService;
import com.example.ai_doc.service.validation.DocumentFileValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Coordinates validation, document understanding, mapping, and workbook generation. */
@Service
public class DocumentProcessingService {

    private static final String COMPLETED_FILENAME = "completed-document.xlsx";

    private final DocumentFileValidator documentFileValidator;
    private final ExcelService excelService;
    private final DocumentUnderstandingService documentUnderstandingService;
    private final HeaderFieldMapper headerFieldMapper;

    public DocumentProcessingService(DocumentFileValidator documentFileValidator,
                                     ExcelService excelService,
                                     DocumentUnderstandingService documentUnderstandingService,
                                     HeaderFieldMapper headerFieldMapper) {
        this.documentFileValidator = documentFileValidator;
        this.excelService = excelService;
        this.documentUnderstandingService = documentUnderstandingService;
        this.headerFieldMapper = headerFieldMapper;
    }

    public ProcessedExcelFile process(MultipartFile document, MultipartFile template) {
        documentFileValidator.validate(document);
        ExcelTemplateInfo templateInfo = excelService.readHeaders(template);
        ExtractedDocumentData extractedDocumentData = documentUnderstandingService.extractFields(document);
        Map<Integer, String> valuesByColumn = headerFieldMapper.mapToColumns(templateInfo, extractedDocumentData);
        byte[] completedWorkbook = excelService.populateTemplate(template, templateInfo, valuesByColumn);

        return new ProcessedExcelFile(COMPLETED_FILENAME, completedWorkbook);
    }
}
