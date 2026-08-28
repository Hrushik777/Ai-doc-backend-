package com.example.ai_doc.service.processing;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.explain.ExplainedField;
import com.example.ai_doc.model.explain.ExplainedMapping;
import com.example.ai_doc.model.explain.ProcessExplanation;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.model.mapping.DeterministicMappingResult;
import com.example.ai_doc.model.mapping.MappingSource;
import com.example.ai_doc.model.mapping.ResolvedFieldMapping;
import com.example.ai_doc.model.mapping.SemanticMapping;
import com.example.ai_doc.model.processing.BatchItemResult;
import com.example.ai_doc.model.processing.BatchProcessedExcelFile;
import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.globalexception.DocumentProcessingException;
import com.example.ai_doc.globalexception.EmptyFileException;
import com.example.ai_doc.globalexception.ExternalAiServiceException;
import com.example.ai_doc.globalexception.NoExcelMappingsException;
import com.example.ai_doc.service.excel.ExcelService;
import com.example.ai_doc.service.mapping.HeaderFieldMapper;
import com.example.ai_doc.service.mapping.SemanticMappingService;
import com.example.ai_doc.service.understanding.DocumentPageImage;
import com.example.ai_doc.service.understanding.DocumentUnderstandingService;
import com.example.ai_doc.service.understanding.PdfDocumentRenderer;
import com.example.ai_doc.service.validation.DocumentFileValidator;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Coordinates validation, document understanding, mapping, and workbook generation. */
@Service
public class DocumentProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessingService.class);

    private static final String COMPLETED_FILENAME = "completed-document.xlsx";

    private final DocumentFileValidator documentFileValidator;
    private final ExcelService excelService;
    private final DocumentUnderstandingService documentUnderstandingService;
    private final HeaderFieldMapper headerFieldMapper;
    private final SemanticMappingService semanticMappingService;
    private final PdfDocumentRenderer pdfDocumentRenderer;

    @Autowired
    public DocumentProcessingService(DocumentFileValidator documentFileValidator,
                                     ExcelService excelService,
                                     DocumentUnderstandingService documentUnderstandingService,
                                     HeaderFieldMapper headerFieldMapper,
                                     SemanticMappingService semanticMappingService,
                                     PdfDocumentRenderer pdfDocumentRenderer) {
        this.documentFileValidator = documentFileValidator;
        this.excelService = excelService;
        this.documentUnderstandingService = documentUnderstandingService;
        this.headerFieldMapper = headerFieldMapper;
        this.semanticMappingService = semanticMappingService;
        this.pdfDocumentRenderer = pdfDocumentRenderer;
    }

    /**
     * Retained so existing callers and tests that predate the explanation endpoint keep
     * compiling; the renderer is only needed by {@link #explain}.
     */
    public DocumentProcessingService(DocumentFileValidator documentFileValidator,
                                     ExcelService excelService,
                                     DocumentUnderstandingService documentUnderstandingService,
                                     HeaderFieldMapper headerFieldMapper,
                                     SemanticMappingService semanticMappingService) {
        this(documentFileValidator, excelService, documentUnderstandingService,
                headerFieldMapper, semanticMappingService, new PdfDocumentRenderer());
    }

    public ProcessedExcelFile process(MultipartFile document, MultipartFile template) {

        documentFileValidator.validate(document);

        try (Workbook workbook = excelService.openWorkbook(template)) {
            long start = System.nanoTime();
            ExcelTemplateInfo templateInfo = excelService.readHeaders(workbook);
            long excelReadTime = elapsedMillis(start);

            start = System.nanoTime();
            DocumentMapping mapping = computeValuesForDocument(document, templateInfo);
            long pipelineTime = elapsedMillis(start);

            Map<Integer, String> valuesByColumn = mapping.valuesByColumn();

            if (LOGGER.isDebugEnabled()) {
                valuesByColumn.forEach((column, value) ->
                        LOGGER.debug("Resolved column {} -> {}", column, value));
            }

            if (mapping.isEmpty()) {
                throw new NoExcelMappingsException(mapping.describeEmptyOutcome());
            }

            start = System.nanoTime();
            excelService.writeRow(workbook, templateInfo, templateInfo.dataRowIndex(), valuesByColumn);
            byte[] completedWorkbook = excelService.serialize(workbook);
            long excelWriteTime = elapsedMillis(start);

            LOGGER.debug("Pipeline timing - excel read {} ms, extract + mapping {} ms, excel write {} ms",
                    excelReadTime, pipelineTime, excelWriteTime);

            return new ProcessedExcelFile(COMPLETED_FILENAME, completedWorkbook);
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to process document", exception);
        }
    }

    /**
     * Runs the same pipeline as {@link #process} and returns the resulting workbook together
     * with the evidence behind it: the fields read from the document, and which column each
     * value landed in, by which stage, with what confidence.
     *
     * <p>The workbook bytes are identical to what {@code process} produces for the same input;
     * nothing here changes how values are resolved.
     */
    public ProcessExplanation explain(MultipartFile document, MultipartFile template) {

        documentFileValidator.validate(document);

        try (Workbook workbook = excelService.openWorkbook(template)) {
            ExcelTemplateInfo templateInfo = excelService.readHeaders(workbook);
            DocumentMapping mapping = computeValuesForDocument(document, templateInfo);

            if (mapping.isEmpty()) {
                throw new NoExcelMappingsException(mapping.describeEmptyOutcome());
            }

            excelService.writeRow(workbook, templateInfo,
                    templateInfo.dataRowIndex(), mapping.valuesByColumn());
            byte[] completedWorkbook = excelService.serialize(workbook);

            return new ProcessExplanation(
                    COMPLETED_FILENAME,
                    Base64.getEncoder().encodeToString(completedWorkbook),
                    templateInfo.headers(),
                    toExplainedFields(mapping.extractedFields()),
                    toExplainedMappings(mapping),
                    renderPageImages(document));
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to process document", exception);
        }
    }

    private List<ExplainedField> toExplainedFields(List<ExtractedField> fields) {
        List<ExplainedField> explained = new ArrayList<>(fields.size());
        for (int index = 0; index < fields.size(); index++) {
            ExtractedField field = fields.get(index);
            explained.add(new ExplainedField(
                    index,
                    field.name(),
                    field.value(),
                    field.pageNumber(),
                    field.x(),
                    field.y(),
                    field.width(),
                    field.height(),
                    field.sourceType()));
        }
        return explained;
    }

    private List<ExplainedMapping> toExplainedMappings(DocumentMapping mapping) {
        // A semantic mapping is rebuilt without its document index, so the reason and the
        // originating fieldId have to come from the semantic stage's own output. Match on
        // the value as well as the column, since several candidates can target one column
        // and only the one that won is worth explaining.
        Map<Integer, List<SemanticMapping>> semanticByColumn = new LinkedHashMap<>();
        for (SemanticMapping semanticMapping : mapping.semanticMappings()) {
            semanticByColumn
                    .computeIfAbsent(semanticMapping.columnIndex(), key -> new ArrayList<>(1))
                    .add(semanticMapping);
        }

        List<ExplainedMapping> explained = new ArrayList<>(mapping.resolvedByColumn().size());

        for (ResolvedFieldMapping resolved : mapping.resolvedByColumn().values()) {
            SemanticMapping origin = null;

            if (resolved.source() == MappingSource.SEMANTIC) {
                List<SemanticMapping> candidates =
                        semanticByColumn.getOrDefault(resolved.columnIndex(), List.of());
                for (SemanticMapping candidate : candidates) {
                    if (Objects.equals(candidate.value(), resolved.value())) {
                        origin = candidate;
                        break;
                    }
                }
                if (origin == null && !candidates.isEmpty()) {
                    origin = candidates.get(0);
                }
            }

            explained.add(new ExplainedMapping(
                    resolved.fieldIndex(),
                    origin != null ? origin.fieldId() : null,
                    resolved.columnIndex(),
                    resolved.value(),
                    resolved.confidence(),
                    resolved.source().name(),
                    origin != null ? origin.reason() : null));
        }

        return explained;
    }

    /**
     * Re-renders the source pages so the client can draw the extracted boxes over the same
     * raster the coordinates were computed against. This is a second render pass: the
     * extraction stage consumes its pages one at a time and does not retain them.
     */
    private List<String> renderPageImages(MultipartFile document) {
        try {
            if (MediaType.APPLICATION_PDF_VALUE.equals(document.getContentType())) {
                List<String> images = new ArrayList<>();
                for (DocumentPageImage page : pdfDocumentRenderer.renderPages(document.getBytes())) {
                    images.add(Base64.getEncoder().encodeToString(page.content()));
                }
                return images;
            }
            return List.of(Base64.getEncoder().encodeToString(document.getBytes()));
        } catch (IOException | RuntimeException exception) {
            // The mapping data is the point of this endpoint; losing the page preview
            // degrades the view but must not fail a document that processed correctly.
            LOGGER.warn("Could not render page images for the explanation view: {}",
                    exception.getMessage());
            return List.of();
        }
    }

    /**
     * Processes each document independently through the existing pipeline (extraction,
     * deterministic mapping, semantic mapping fallback), writing one row per document into
     * the same open workbook. A document that fails does not abort the batch: its row instead
     * gets a failure marker, and processing continues with the remaining documents.
     */
    public BatchProcessedExcelFile processBatch(List<MultipartFile> documents, MultipartFile template) {

        if (documents == null || documents.isEmpty()) {
            throw new EmptyFileException("At least one document must be provided");
        }

        try (Workbook workbook = excelService.openWorkbook(template)) {
            ExcelTemplateInfo templateInfo = excelService.readHeaders(workbook);

            int firstColumnIndex = templateInfo.headers().stream()
                    .mapToInt(ExcelColumn::columnIndex)
                    .min()
                    .orElse(0);

            List<BatchItemResult> results = new ArrayList<>(documents.size());
            int rowIndex = templateInfo.dataRowIndex();

            for (MultipartFile document : documents) {
                String filename = document.getOriginalFilename();

                try {
                    documentFileValidator.validate(document);
                    DocumentMapping mapping = computeValuesForDocument(document, templateInfo);

                    if (mapping.isEmpty()) {
                        throw new NoExcelMappingsException(mapping.describeEmptyOutcome());
                    }

                    excelService.writeRow(workbook, templateInfo, rowIndex, mapping.valuesByColumn());
                    results.add(new BatchItemResult(filename, true, rowIndex, null));
                } catch (RuntimeException exception) {
                    excelService.writeRow(workbook, templateInfo, rowIndex,
                            Map.of(firstColumnIndex, "PROCESSING FAILED: " + exception.getMessage()));
                    results.add(new BatchItemResult(filename, false, rowIndex, exception.getMessage()));
                }

                rowIndex++;
            }

            return new BatchProcessedExcelFile(COMPLETED_FILENAME, excelService.serialize(workbook), results);
        } catch (IOException exception) {
            throw new DocumentProcessingException("Failed to process document batch", exception);
        }
    }

    /**
     * Outcome of the per-document pipeline. Carries the stage counts alongside the resolved
     * values so callers can report *why* a document produced nothing - "the parse model
     * returned no fields" and "fields were returned but none matched a header" are different
     * problems with different fixes, and reporting them identically hides the real one.
     */
    private record DocumentMapping(Map<Integer, String> valuesByColumn,
                                   Map<Integer, ResolvedFieldMapping> resolvedByColumn,
                                   List<ExtractedField> extractedFields,
                                   List<SemanticMapping> semanticMappings,
                                   int extractedFieldCount,
                                   int deterministicMatchCount,
                                   int unmatchedFieldCount,
                                   boolean semanticStageUsed) {

        boolean isEmpty() {
            return valuesByColumn.isEmpty();
        }

        String describeEmptyOutcome() {
            if (extractedFieldCount == 0) {
                return "Document understanding returned no fields for this document"
                        + " - the page may be blank, too low quality to read, or in a layout"
                        + " the parse model did not recognise";
            }
            return "No extracted document fields matched the Excel template headers"
                    + " (" + extractedFieldCount + " fields extracted, none matched"
                    + (semanticStageUsed ? " deterministically or semantically)" : " deterministically)");
        }
    }

    private DocumentMapping computeValuesForDocument(MultipartFile document, ExcelTemplateInfo templateInfo) {
        ExtractedDocumentData extractedDocumentData =
                documentUnderstandingService.extractFields(document);

        DeterministicMappingResult deterministicMappings =
                headerFieldMapper.findExactMatches(templateInfo, extractedDocumentData);

        // Only columns the deterministic stage could not fill are still in play. A semantic
        // mapping onto an already-resolved column is always discarded by choosePreferredMapping
        // (deterministic confidence is 1.0 and wins ties), so sending those headers to the LLM
        // can only cost tokens and latency - it can never change the output.
        List<ExcelColumn> unresolvedHeaders = unresolvedHeaders(templateInfo, deterministicMappings);

        boolean semanticFallbackCanContribute =
                !deterministicMappings.unmatchedFields().isEmpty() && !unresolvedHeaders.isEmpty();

        List<SemanticMapping> semanticMappings = List.of();

        if (semanticFallbackCanContribute) {
            try {
                semanticMappings = semanticMappingService.mapUnmatchedFields(
                        deterministicMappings.unmatchedFields(),
                        unresolvedHeaders);
            } catch (DocumentProcessingException | ExternalAiServiceException exception) {
                // The semantic stage is an optional enrichment on top of deterministic matches
                // that are already trustworthy. If the model misbehaves or the service is
                // unreachable, degrade to the deterministic columns instead of throwing away
                // a document we could still partially fill.
                LOGGER.warn("Semantic mapping stage failed; continuing with deterministic mappings only: {}",
                        exception.getMessage());
            }
        }

        Map<Integer, ResolvedFieldMapping> resolvedByColumn =
                resolveMappingsByColumn(deterministicMappings, semanticMappings, extractedDocumentData);

        Map<Integer, String> valuesByColumn = toValuesByColumn(resolvedByColumn);

        // One summary line per document: without it, a document that resolves nothing looks
        // identical whether extraction, matching, or the semantic stage was responsible.
        LOGGER.info("Mapping summary for {}: extracted={} deterministic={} unmatched={} semanticStage={} resolved={}",
                document.getOriginalFilename(),
                extractedDocumentData.fields().size(),
                deterministicMappings.mappingsByColumn().size(),
                deterministicMappings.unmatchedFields().size(),
                semanticFallbackCanContribute ? "called" : "skipped",
                valuesByColumn.size());

        return new DocumentMapping(
                valuesByColumn,
                resolvedByColumn,
                extractedDocumentData.fields(),
                semanticMappings,
                extractedDocumentData.fields().size(),
                deterministicMappings.mappingsByColumn().size(),
                deterministicMappings.unmatchedFields().size(),
                semanticFallbackCanContribute);
    }

    private List<ExcelColumn> unresolvedHeaders(ExcelTemplateInfo templateInfo,
                                                DeterministicMappingResult deterministicMappings) {
        Set<Integer> resolvedColumns = deterministicMappings.mappingsByColumn().keySet();

        if (resolvedColumns.isEmpty()) {
            return templateInfo.headers();
        }

        List<ExcelColumn> unresolved = new ArrayList<>();
        for (ExcelColumn header : templateInfo.headers()) {
            if (!resolvedColumns.contains(header.columnIndex())) {
                unresolved.add(header);
            }
        }
        return unresolved;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * Resolves one winning mapping per column. Returns the mappings themselves rather
     * than only their values: the source, confidence and field behind each cell are
     * what the mapping visualization renders, and collapsing to strings here is what
     * previously discarded them.
     */
    private Map<Integer, ResolvedFieldMapping> resolveMappingsByColumn(
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

        return resolvedMappings;
    }

    private static Map<Integer, String> toValuesByColumn(
            Map<Integer, ResolvedFieldMapping> resolvedMappings) {

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
