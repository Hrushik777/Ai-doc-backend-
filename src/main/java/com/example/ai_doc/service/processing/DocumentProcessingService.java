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

        long start;

        start = System.nanoTime();
        ExcelTemplateInfo templateInfo = excelService.readHeaders(template);
        long excelReadTime = elapsedMillis(start);

        start = System.nanoTime();
        ExtractedDocumentData extractedDocumentData =
                documentUnderstandingService.extractFields(document);
        long extractionTime = elapsedMillis(start);

        start = System.nanoTime();
        DeterministicMappingResult deterministicMappings =
                headerFieldMapper.findExactMatches(
                        templateInfo,
                        extractedDocumentData
                );

        System.out.println("===== MAPPING STATS =====");
        System.out.println(
                "Extracted fields: "
                        + extractedDocumentData.fields().size()
        );
        System.out.println(
                "Deterministic matches: "
                        + deterministicMappings.mappingsByColumn().size()
        );
        System.out.println(
                "Unmatched fields: "
                        + deterministicMappings.unmatchedFields().size()
        );
        System.out.println("=========================");
        long deterministicTime = elapsedMillis(start);

        start = System.nanoTime();
        List<SemanticMapping> semanticMappings =
                deterministicMappings.unmatchedFields().isEmpty()
                        ? List.of()
                        : semanticMappingService.mapUnmatchedFields(
                        deterministicMappings.unmatchedFields(),
                        templateInfo.headers()
                );
        long semanticTime = elapsedMillis(start);

        start = System.nanoTime();
        Map<Integer, String> valuesByColumn =
                resolveValuesByColumn(
                        deterministicMappings,
                        semanticMappings,
                        extractedDocumentData
                );
        long resolveTime = elapsedMillis(start);

        System.out.println("===== FINAL VALUES BY COLUMN =====");

        valuesByColumn.forEach((column, value) ->
                System.out.println(
                        "COLUMN " + column + " -> " + value
                ));

        System.out.println("==================================");

        if (valuesByColumn.isEmpty()) {
            throw new NoExcelMappingsException(
                    "No extracted document fields matched the Excel template headers"
            );
        }

        start = System.nanoTime();
        byte[] completedWorkbook =
                excelService.populateTemplate(
                        template,
                        templateInfo,
                        valuesByColumn
                );
        long excelWriteTime = elapsedMillis(start);

        System.out.println("===== PIPELINE TIMING =====");
        System.out.println("Excel read:       " + excelReadTime + " ms");
        System.out.println("Document parse:   " + extractionTime + " ms");
        System.out.println("Deterministic:    " + deterministicTime + " ms");
        System.out.println("Semantic mapping: " + semanticTime + " ms");
        System.out.println("Resolve values:   " + resolveTime + " ms");
        System.out.println("Excel write:      " + excelWriteTime + " ms");
        System.out.println("===========================");

        return new ProcessedExcelFile(
                COMPLETED_FILENAME,
                completedWorkbook
        );
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private Map<Integer, String> resolveValuesByColumn(
            DeterministicMappingResult deterministicMappings,
            List<SemanticMapping> semanticMappings,
            ExtractedDocumentData extractedDocumentData) {

        Map<Integer, ResolvedFieldMapping> resolvedMappings =
                new LinkedHashMap<>(deterministicMappings.mappingsByColumn());

        for (SemanticMapping semanticMapping : semanticMappings) {

            String value = semanticMapping.value();

            if (value == null || value.isBlank()) {
                continue;
            }

            ResolvedFieldMapping candidate = new ResolvedFieldMapping(
                    -1,
                    semanticMapping.columnIndex(),
                    value,
                    semanticMapping.confidence(),
                    MappingSource.SEMANTIC
            );

            resolvedMappings.merge(
                    candidate.columnIndex(),
                    candidate,
                    this::choosePreferredMapping
            );
        }

        Map<Integer, String> valuesByColumn = new LinkedHashMap<>();

        for (ResolvedFieldMapping mapping : resolvedMappings.values()) {
            valuesByColumn.put(
                    mapping.columnIndex(),
                    mapping.value()
            );
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
