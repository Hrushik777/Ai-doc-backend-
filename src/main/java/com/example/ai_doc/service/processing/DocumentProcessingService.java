package com.example.ai_doc.service.processing;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.explain.ExplainedField;
import com.example.ai_doc.model.explain.ExplainedMapping;
import com.example.ai_doc.model.explain.ProcessExplanation;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.model.layout.DocumentLayout;
import com.example.ai_doc.model.layout.ParsedDocument;
import com.example.ai_doc.model.mapping.DeterministicMappingResult;
import com.example.ai_doc.model.mapping.CellOrigin;
import com.example.ai_doc.model.mapping.MappedRecord;
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
import com.example.ai_doc.service.layout.ColumnClusterer;
import com.example.ai_doc.service.layout.ColumnGutterDetector;
import com.example.ai_doc.service.layout.LayoutAnalyzer;
import com.example.ai_doc.service.layout.RegionClassifier;
import com.example.ai_doc.service.layout.RegionContinuationDetector;
import com.example.ai_doc.service.layout.RowBander;
import com.example.ai_doc.service.layout.VerticalSlabSplitter;
import com.example.ai_doc.service.mapping.HeaderFieldMapper;
import com.example.ai_doc.service.mapping.HeaderInferenceService;
import com.example.ai_doc.service.mapping.LayoutHeaderInferrer;
import com.example.ai_doc.service.mapping.LayoutRecordMapper;
import com.example.ai_doc.service.mapping.SemanticMappingService;
import com.example.ai_doc.service.understanding.DocumentPageImage;
import com.example.ai_doc.service.understanding.DocumentUnderstandingService;
import com.example.ai_doc.service.understanding.ParsedDocumentFlattener;
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
import java.util.HashSet;
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
    private final LayoutAnalyzer layoutAnalyzer;
    private final LayoutRecordMapper layoutRecordMapper;
    private final ParsedDocumentFlattener parsedDocumentFlattener;
    private final HeaderInferenceService headerInferenceService;

    @Autowired
    public DocumentProcessingService(DocumentFileValidator documentFileValidator,
                                     ExcelService excelService,
                                     DocumentUnderstandingService documentUnderstandingService,
                                     HeaderFieldMapper headerFieldMapper,
                                     SemanticMappingService semanticMappingService,
                                     PdfDocumentRenderer pdfDocumentRenderer,
                                     LayoutAnalyzer layoutAnalyzer,
                                     LayoutRecordMapper layoutRecordMapper,
                                     ParsedDocumentFlattener parsedDocumentFlattener,
                                     HeaderInferenceService headerInferenceService) {
        this.documentFileValidator = documentFileValidator;
        this.excelService = excelService;
        this.documentUnderstandingService = documentUnderstandingService;
        this.headerFieldMapper = headerFieldMapper;
        this.semanticMappingService = semanticMappingService;
        this.pdfDocumentRenderer = pdfDocumentRenderer;
        this.layoutAnalyzer = layoutAnalyzer;
        this.layoutRecordMapper = layoutRecordMapper;
        this.parsedDocumentFlattener = parsedDocumentFlattener;
        this.headerInferenceService = headerInferenceService;
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
                headerFieldMapper, semanticMappingService, new PdfDocumentRenderer(),
                defaultLayoutAnalyzer(), new LayoutRecordMapper(headerFieldMapper),
                new ParsedDocumentFlattener(), new LayoutHeaderInferrer()::infer);
    }

    /** The layout stage has no configuration, so the legacy constructor can build its own. */
    private static LayoutAnalyzer defaultLayoutAnalyzer() {
        RowBander rowBander = new RowBander();
        return new LayoutAnalyzer(
                rowBander,
                new VerticalSlabSplitter(rowBander),
                new ColumnGutterDetector(),
                new ColumnClusterer(),
                new RegionClassifier(),
                new RegionContinuationDetector());
    }

    public ProcessedExcelFile process(MultipartFile document, MultipartFile template) {

        documentFileValidator.validate(document);

        long start = System.nanoTime();
        PreparedWorkbook prepared = prepareWorkbook(document, template);
        long excelReadTime = elapsedMillis(start);

        try (Workbook workbook = prepared.workbook()) {
            ExcelTemplateInfo templateInfo = prepared.templateInfo();

            start = System.nanoTime();
            DocumentMapping mapping = mapDocument(document, prepared);
            long pipelineTime = elapsedMillis(start);

            if (LOGGER.isDebugEnabled()) {
                for (int recordIndex = 0; recordIndex < mapping.records().size(); recordIndex++) {
                    int row = recordIndex;
                    mapping.records().get(recordIndex).values().forEach((column, value) ->
                            LOGGER.debug("Record {} resolved column {} -> {}", row, column, value));
                }
            }

            if (mapping.isEmpty()) {
                throw new NoExcelMappingsException(mapping.describeEmptyOutcome());
            }

            start = System.nanoTime();
            excelService.writeRows(workbook, templateInfo, templateInfo.dataRowIndex(), mapping.valueRows());
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

        PreparedWorkbook prepared = prepareWorkbook(document, template);

        try (Workbook workbook = prepared.workbook()) {
            ExcelTemplateInfo templateInfo = prepared.templateInfo();
            DocumentMapping mapping = mapDocument(document, prepared);

            if (mapping.isEmpty()) {
                throw new NoExcelMappingsException(mapping.describeEmptyOutcome());
            }

            excelService.writeRows(workbook, templateInfo,
                    templateInfo.dataRowIndex(), mapping.valueRows());
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

    /**
     * Explains every cell of every output row: which stage put the value there, and where it
     * came from.
     *
     * <p>Two provenance shapes are folded together here. A structurally mapped value was read
     * from a position in the layout and carries that rectangle directly - it has no entry in
     * the flat extracted-field list to point at. A deterministic or semantic value points
     * back at the field it came from, and its geometry is looked up there.
     */
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

        List<ExplainedMapping> explained = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < mapping.records().size(); rowIndex++) {
            MappedRecord record = mapping.records().get(rowIndex);

            for (Map.Entry<Integer, String> cell : record.values().entrySet()) {
                CellOrigin origin = record.origins().get(cell.getKey());

                if (origin != null) {
                    explained.add(structuralMapping(rowIndex, cell.getKey(), cell.getValue(), origin));
                    continue;
                }

                ResolvedFieldMapping resolved = mapping.resolvedByColumn().get(cell.getKey());
                if (resolved != null) {
                    explained.add(flatMapping(rowIndex, resolved, semanticByColumn, mapping.extractedFields()));
                }
            }
        }

        return explained;
    }

    private ExplainedMapping structuralMapping(int rowIndex, int columnIndex, String value, CellOrigin origin) {
        return new ExplainedMapping(
                rowIndex,
                -1,
                null,
                columnIndex,
                value,
                1.0,
                MappingSource.STRUCTURAL.name(),
                origin.reason(),
                origin.page(),
                origin.bbox().xmin(),
                origin.bbox().ymin(),
                origin.bbox().width(),
                origin.bbox().height());
    }

    private ExplainedMapping flatMapping(int rowIndex,
                                         ResolvedFieldMapping resolved,
                                         Map<Integer, List<SemanticMapping>> semanticByColumn,
                                         List<ExtractedField> extractedFields) {
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

        String fieldId = origin != null ? origin.fieldId() : null;
        ExtractedField field = fieldFor(resolved.fieldIndex(), fieldId, extractedFields);

        return new ExplainedMapping(
                rowIndex,
                resolved.fieldIndex(),
                fieldId,
                resolved.columnIndex(),
                resolved.value(),
                resolved.confidence(),
                resolved.source().name(),
                origin != null ? origin.reason() : null,
                field != null ? field.pageNumber() : null,
                field != null ? field.x() : null,
                field != null ? field.y() : null,
                field != null ? field.width() : null,
                field != null ? field.height() : null);
    }

    /**
     * The field a mapping came from. A deterministic mapping knows its index outright; a
     * semantic one only kept the id it was given, so the index is read back out of that -
     * which is what lets a semantically mapped cell still be drawn on the page preview.
     */
    private ExtractedField fieldFor(int fieldIndex, String fieldId, List<ExtractedField> extractedFields) {
        if (fieldIndex >= 0 && fieldIndex < extractedFields.size()) {
            return extractedFields.get(fieldIndex);
        }
        if (fieldId == null || !fieldId.startsWith("field-")) {
            return null;
        }

        // "field-3" and "field-3-1" both originate in field 3; the suffix distinguishes
        // several values pulled out of that one element.
        String[] parts = fieldId.substring("field-".length()).split("-");
        try {
            int index = Integer.parseInt(parts[0]);
            return index >= 0 && index < extractedFields.size() ? extractedFields.get(index) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
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

        // With no template the columns come from the first document, so every later
        // document in the batch is filled against the same inferred header row.
        documentFileValidator.validate(documents.get(0));
        PreparedWorkbook prepared = prepareWorkbook(documents.get(0), template);

        try (Workbook workbook = prepared.workbook()) {
            ExcelTemplateInfo templateInfo = prepared.templateInfo();

            int firstColumnIndex = templateInfo.headers().stream()
                    .mapToInt(ExcelColumn::columnIndex)
                    .min()
                    .orElse(0);

            List<BatchItemResult> results = new ArrayList<>(documents.size());
            int rowIndex = templateInfo.dataRowIndex();

            for (int documentIndex = 0; documentIndex < documents.size(); documentIndex++) {
                MultipartFile document = documents.get(documentIndex);
                String filename = document.getOriginalFilename();

                try {
                    documentFileValidator.validate(document);
                    // The first document may already have been parsed to infer the headers;
                    // reusing that parse keeps the batch to one model call per document.
                    DocumentMapping mapping = documentIndex == 0
                            ? mapDocument(document, prepared)
                            : computeRecordsForDocument(document, templateInfo);

                    if (mapping.isEmpty()) {
                        throw new NoExcelMappingsException(mapping.describeEmptyOutcome());
                    }

                    // A document that yielded a table contributes several rows, so the next
                    // document has to start below all of them rather than one row down.
                    int written = excelService.writeRows(workbook, templateInfo, rowIndex, mapping.valueRows());
                    results.add(new BatchItemResult(filename, true, rowIndex, null));
                    rowIndex += written;
                } catch (RuntimeException exception) {
                    excelService.writeRow(workbook, templateInfo, rowIndex,
                            Map.of(firstColumnIndex, "PROCESSING FAILED: " + exception.getMessage()));
                    results.add(new BatchItemResult(filename, false, rowIndex, exception.getMessage()));
                    rowIndex++;
                }
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
    private record DocumentMapping(List<MappedRecord> records,
                                   Map<Integer, ResolvedFieldMapping> resolvedByColumn,
                                   List<ExtractedField> extractedFields,
                                   List<SemanticMapping> semanticMappings,
                                   int extractedFieldCount,
                                   int deterministicMatchCount,
                                   int unmatchedFieldCount,
                                   boolean semanticStageUsed) {

        boolean isEmpty() {
            return records.isEmpty();
        }

        /** Just the values, for writing; provenance is only needed by the explanation view. */
        List<Map<Integer, String>> valueRows() {
            return records.stream().map(MappedRecord::values).toList();
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

    /**
     * Runs the full pipeline for one document: parse it with geometry intact, derive its
     * structure from those coordinates, map what the structure alone settles, and call the
     * semantic stage only for what is genuinely left over.
     */
    private DocumentMapping computeRecordsForDocument(MultipartFile document, ExcelTemplateInfo templateInfo) {
        ParsedDocument parsed = safeParse(document);
        return computeRecords(document, parsed, analyzeLayout(parsed), templateInfo);
    }

    /**
     * A provider that reports no geometry - or a stub implementing only the flat API - still
     * works. There is simply no layout to reason about, and the document follows the
     * single-record path it always did.
     */
    private DocumentLayout analyzeLayout(ParsedDocument parsed) {
        return parsed.isEmpty()
                ? DocumentLayout.empty()
                : layoutAnalyzer.analyze(parsed.elements(), parsed.pages());
    }

    private DocumentMapping computeRecords(MultipartFile document,
                                           ParsedDocument parsed,
                                           DocumentLayout layout,
                                           ExcelTemplateInfo templateInfo) {

        ExtractedDocumentData extractedDocumentData = parsed.isEmpty()
                ? documentUnderstandingService.extractFields(document)
                : parsedDocumentFlattener.flatten(parsed);

        // Structural mapping runs first and costs nothing: a table whose header band matches
        // the template resolves every one of its rows without a model call.
        List<MappedRecord> layoutRecords = layoutRecordMapper.mapLayout(templateInfo, layout);

        DeterministicMappingResult deterministicMappings =
                headerFieldMapper.findExactMatches(templateInfo, extractedDocumentData);

        // Only columns that nothing has filled yet are still in play. A semantic mapping onto
        // an already-resolved column is always discarded by choosePreferredMapping, so sending
        // those headers to the LLM can only cost tokens and latency - it can never change the
        // output. Structural records count as resolved for exactly the same reason.
        Set<Integer> resolvedColumns = new HashSet<>(deterministicMappings.mappingsByColumn().keySet());
        for (MappedRecord record : layoutRecords) {
            resolvedColumns.addAll(record.values().keySet());
        }

        List<ExcelColumn> unresolvedHeaders = unresolvedHeaders(templateInfo, resolvedColumns);

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

        List<MappedRecord> records =
                combineRecords(layoutRecords, toValuesByColumn(resolvedByColumn));

        // One summary line per document: without it, a document that resolves nothing looks
        // identical whether extraction, structure, matching, or the semantic stage was
        // responsible.
        LOGGER.info("Mapping summary for {}: extracted={} regions={} structural={} deterministic={}"
                        + " unmatched={} semanticStage={} rows={}",
                document.getOriginalFilename(),
                extractedDocumentData.fields().size(),
                layout.regions().size(),
                layoutRecords.size(),
                deterministicMappings.mappingsByColumn().size(),
                deterministicMappings.unmatchedFields().size(),
                semanticFallbackCanContribute ? "called" : "skipped",
                records.size());

        return new DocumentMapping(
                records,
                resolvedByColumn,
                extractedDocumentData.fields(),
                semanticMappings,
                extractedDocumentData.fields().size(),
                deterministicMappings.mappingsByColumn().size(),
                deterministicMappings.unmatchedFields().size(),
                semanticFallbackCanContribute);
    }

    /**
     * The workbook to fill, its columns, and - when no template was supplied and the columns
     * had to be inferred - the parse those columns came from.
     *
     * <p>Carrying the parse matters: inferring headers requires reading the document, and
     * without this the same document would be sent to the parse model a second time to be
     * mapped, doubling the cost of every templateless request.
     */
    private record PreparedWorkbook(Workbook workbook,
                                    ExcelTemplateInfo templateInfo,
                                    ParsedDocument parsed,
                                    DocumentLayout layout) {

        boolean headersWereInferred() {
            return parsed != null;
        }
    }

    /**
     * Opens the supplied template, or - when none was supplied - reads the document, works
     * out what its columns should be, and builds a workbook around them.
     */
    private PreparedWorkbook prepareWorkbook(MultipartFile document, MultipartFile template) {
        if (template != null) {
            Workbook workbook = excelService.openWorkbook(template);
            return new PreparedWorkbook(workbook, excelService.readHeaders(workbook), null, null);
        }

        ParsedDocument parsed = safeParse(document);
        DocumentLayout layout = analyzeLayout(parsed);
        List<String> headers = headerInferenceService.inferHeaders(layout);

        if (headers.isEmpty()) {
            throw new NoExcelMappingsException(
                    "No Excel template was supplied and the document did not yield any columns"
                            + " to build one from - it may be blank, too low quality to read, or"
                            + " in a layout the parse model did not recognise");
        }

        LOGGER.info("No template supplied for {}; inferred {} columns: {}",
                document.getOriginalFilename(), headers.size(), headers);

        ExcelService.SynthesizedTemplate synthesized = excelService.createWorkbook(headers);
        return new PreparedWorkbook(synthesized.workbook(), synthesized.templateInfo(), parsed, layout);
    }

    private DocumentMapping mapDocument(MultipartFile document, PreparedWorkbook prepared) {
        return prepared.headersWereInferred()
                ? computeRecords(document, prepared.parsed(), prepared.layout(), prepared.templateInfo())
                : computeRecordsForDocument(document, prepared.templateInfo());
    }

    private ParsedDocument safeParse(MultipartFile document) {
        ParsedDocument parsed = documentUnderstandingService.parse(document);
        return parsed == null ? ParsedDocument.empty() : parsed;
    }

    /**
     * Folds the flat mapping results into the structural records. Values that describe the
     * whole document - a labelled field above a table, or anything the semantic stage
     * resolved - fill columns a row left empty, but never overwrite what the row itself said.
     */
    private List<MappedRecord> combineRecords(List<MappedRecord> layoutRecords,
                                              Map<Integer, String> flatValues) {
        if (layoutRecords.isEmpty()) {
            return flatValues.isEmpty() ? List.of() : List.of(MappedRecord.of(flatValues));
        }

        List<MappedRecord> records = new ArrayList<>(layoutRecords.size());
        for (MappedRecord layoutRecord : layoutRecords) {
            records.add(layoutRecord.withDefaults(flatValues));
        }
        return records;
    }

    private List<ExcelColumn> unresolvedHeaders(ExcelTemplateInfo templateInfo,
                                                Set<Integer> resolvedColumns) {
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
