package com.example.ai_doc.service.processing;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.model.mapping.DeterministicMappingResult;
import com.example.ai_doc.model.mapping.MappingSource;
import com.example.ai_doc.model.mapping.ResolvedFieldMapping;
import com.example.ai_doc.model.mapping.SemanticMapping;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.globalexception.NoExcelMappingsException;
import com.example.ai_doc.service.excel.ExcelService;
import com.example.ai_doc.service.mapping.HeaderFieldMapper;
import com.example.ai_doc.service.mapping.SemanticMappingService;
import com.example.ai_doc.service.understanding.DocumentUnderstandingService;
import com.example.ai_doc.service.validation.DocumentFileValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coordinates validation, document understanding, mapping, and workbook generation. */
@Service
public class DocumentProcessingService {

    private static final String COMPLETED_FILENAME = "completed-document.xlsx";

    private final DocumentFileValidator documentFileValidator;
    private final ExcelService excelService;
    private final DocumentUnderstandingService documentUnderstandingService;
    private final HeaderFieldMapper headerFieldMapper;
    private final SemanticMappingService semanticMappingService;

    public DocumentProcessingService(DocumentFileValidator documentFileValidator,
                                     ExcelService excelService,
                                     DocumentUnderstandingService documentUnderstandingService,
                                     HeaderFieldMapper headerFieldMapper,
                                     SemanticMappingService semanticMappingService) {
        this.documentFileValidator = documentFileValidator;
        this.excelService = excelService;
        this.documentUnderstandingService = documentUnderstandingService;
        this.headerFieldMapper = headerFieldMapper;
        this.semanticMappingService = semanticMappingService;
    }

    public ProcessedExcelFile process(MultipartFile document, MultipartFile template) {
        documentFileValidator.validate(document);
        ExcelTemplateInfo templateInfo = excelService.readHeaders(template);
        ExtractedDocumentData extractedDocumentData = documentUnderstandingService.extractFields(document);
        DeterministicMappingResult deterministicMappings = headerFieldMapper
                .findExactMatches(templateInfo, extractedDocumentData);
        List<SemanticMapping> semanticMappings = deterministicMappings.unmatchedFields().isEmpty()
                ? List.of()
                : semanticMappingService.mapUnmatchedFields(
                        deterministicMappings.unmatchedFields(), templateInfo.headers());
        Map<Integer, String> valuesByColumn = resolveValuesByColumn(
                deterministicMappings, semanticMappings, extractedDocumentData);

        if (valuesByColumn.isEmpty()) {
            throw new NoExcelMappingsException("No extracted document fields matched the Excel template headers");
        }
        byte[] completedWorkbook = excelService.populateTemplate(template, templateInfo, valuesByColumn);

        return new ProcessedExcelFile(COMPLETED_FILENAME, completedWorkbook);
    }

    private Map<Integer, String> resolveValuesByColumn(DeterministicMappingResult deterministicMappings,
                                                        List<SemanticMapping> semanticMappings,
                                                        ExtractedDocumentData extractedDocumentData) {
        Map<Integer, ResolvedFieldMapping> resolvedMappings = new LinkedHashMap<>(
                deterministicMappings.mappingsByColumn());

        for (SemanticMapping semanticMapping : semanticMappings) {
            if (semanticMapping.fieldIndex() < 0
                    || semanticMapping.fieldIndex() >= extractedDocumentData.fields().size()) {
                throw new DocumentProcessingException("Semantic mapping referenced an unknown document field");
            }

            String value = extractedDocumentData.fields().get(semanticMapping.fieldIndex()).value();
            if (value == null) {
                continue;
            }

            ResolvedFieldMapping candidate = new ResolvedFieldMapping(
                    semanticMapping.fieldIndex(),
                    semanticMapping.columnIndex(),
                    value,
                    semanticMapping.confidence(),
                    MappingSource.SEMANTIC
            );
            resolvedMappings.merge(candidate.columnIndex(), candidate, this::choosePreferredMapping);
        }

        Map<Integer, String> valuesByColumn = new LinkedHashMap<>();
        for (ResolvedFieldMapping mapping : resolvedMappings.values()) {
            valuesByColumn.put(mapping.columnIndex(), mapping.value());
        }
        return valuesByColumn;
    }

    private ResolvedFieldMapping choosePreferredMapping(ResolvedFieldMapping existing,
                                                         ResolvedFieldMapping candidate) {
        // One Excel cell receives one value: higher confidence wins; ties retain deterministic/earlier data.
        if (candidate.confidence() > existing.confidence()
                || (candidate.confidence() == existing.confidence()
                && candidate.source() == MappingSource.DETERMINISTIC
                && existing.source() != MappingSource.DETERMINISTIC)
                || (candidate.confidence() == existing.confidence()
                && candidate.source() == existing.source()
                && candidate.fieldIndex() < existing.fieldIndex())) {
            return candidate;
        }
        return existing;
    }
}
