# Graph Report - ai-doc  (2026-08-29)

## Corpus Check
- 130 files · ~49,521 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 793 nodes · 2554 edges · 54 communities (43 shown, 11 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 289 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1ae00f43`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DocumentProcessingException
- ExcelService
- org.springframework.http.ResponseEntity
- ExcelColumn
- DocumentLayout
- org.junit.jupiter.api.Test
- What You Must Do When Invoked
- Document
- graphify reference: extra exports and benchmark
- mvnw
- DocumentElement
- LayoutRecordMapper
- AiDocApplication
- com.example:ai-doc
- AI Document to Excel Backend
- graphify reference: query, path, explain
- graphify reference: add a URL and watch a folder
- graphify reference: commit hook and native CLAUDE.md integration
- graphify reference: incremental update and cluster-only
- graphify reference: GitHub clone and cross-repo merge
- graphify reference: transcribe video and audio
- CLAUDE.md
- .claude/CLAUDE.md
- extraction-spec.md
- DocumentProcessingBenchmarkTest
- .sanitize
- CorsConfiguration
- DocumentProcessingService.java
- org.springframework.stereotype.Component
- LayoutRegion
- BBox
- .analyze
- SemanticMappingValidationTest
- LayoutAnalyzerTest.java
- TableCellSplitter

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 55 edges
2. `DocumentProcessingService` - 51 edges
3. `BBox` - 45 edges
4. `ExcelColumn` - 39 edges
5. `ExtractedDocumentData` - 37 edges
6. `ExcelTemplateInfo` - 36 edges
7. `LayoutRegion` - 34 edges
8. `ExcelService` - 31 edges
9. `DocumentProcessingException` - 28 edges
10. `DocumentLayout` - 28 edges

## Surprising Connections (you probably didn't know these)
- `DocumentController` --references--> `DocumentService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/DocumentController.java → src/main/java/com/example/ai_doc/pipeline/document/DocumentService.java
- `ProcessExplanation` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `IndexedExtractedField` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/mapping/IndexedExtractedField.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `DocumentMapping` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `CarriedHeaderBand` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/pipeline/mapping/LayoutRecordMapper.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java

## Import Cycles
- None detected.

## Communities (54 total, 11 thin omitted)

### Community 0 - "DocumentProcessingException"
Cohesion: 0.07
Nodes (25): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, PDFRenderer, DocumentProcessingException, ExternalAiServiceException (+17 more)

### Community 1 - "ExcelService"
Cohesion: 0.09
Nodes (18): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Sheet, org.apache.poi.ss.usermodel.Workbook, org.apache.poi.xssf.usermodel.XSSFWorkbook, InvalidExcelTemplateException, ExcelWriteMode, APPEND_ONLY (+10 more)

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.28
Nodes (8): org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException, ApiErrorResponse, GlobalExceptionHandler

### Community 3 - "ExcelColumn"
Cohesion: 0.06
Nodes (21): org.junit.jupiter.api.condition.EnabledIfSystemProperty, org.springframework.beans.factory.annotation.Autowired, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, ExtractedDocumentData, ExtractedField, ExcelColumn, ExcelTemplateInfo (+13 more)

### Community 4 - "DocumentLayout"
Cohesion: 0.25
Nodes (3): DocumentLayout, LayoutHeaderInferrer, LayoutHeaderInferrerTest

### Community 5 - "org.junit.jupiter.api.Test"
Cohesion: 0.09
Nodes (19): org.junit.jupiter.api.BeforeEach, org.junit.jupiter.api.Test, org.springframework.mock.web.MockMultipartFile, org.springframework.test.web.servlet.MockMvc, AIServiceNotConfiguredException, EmptyFileException, FileSizeExceededException, InvalidFileTypeException (+11 more)

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "Document"
Cohesion: 0.11
Nodes (7): Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, DocumentService, Table

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.15
Nodes (7): DocumentElement, ColumnAssignment, ColumnClusterer, Geometry, LayoutAnalyzer, RowBander, VerticalSlabSplitter

### Community 11 - "LayoutRecordMapper"
Cohesion: 0.11
Nodes (7): MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading, RawFieldRecordBuilder, LayoutRecordMapperTest

### Community 14 - "AI Document to Excel Backend"
Cohesion: 0.22
Nodes (8): AI Document to Excel Backend, Current architecture, Endpoints, Implemented now, Process a document into a template, Requirements and running, Still to configure, Upload a source document

### Community 15 - "graphify reference: query, path, explain"
Cohesion: 0.33
Nodes (5): For /graphify explain, For /graphify path, graphify reference: query, path, explain, Step 0 — Constrained query expansion (REQUIRED before traversal), Step 1 — Traversal

### Community 16 - "graphify reference: add a URL and watch a folder"
Cohesion: 0.50
Nodes (3): For /graphify add, For --watch, graphify reference: add a URL and watch a folder

### Community 17 - "graphify reference: commit hook and native CLAUDE.md integration"
Cohesion: 0.50
Nodes (3): For git commit hook, For native CLAUDE.md integration, graphify reference: commit hook and native CLAUDE.md integration

### Community 18 - "graphify reference: incremental update and cluster-only"
Cohesion: 0.50
Nodes (3): For --cluster-only, For --update (incremental re-extraction), graphify reference: incremental update and cluster-only

### Community 24 - "DocumentProcessingBenchmarkTest"
Cohesion: 0.21
Nodes (5): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.springframework.boot.test.context.SpringBootTest, AiDocApplicationTests, DocumentProcessingBenchmarkTest, MockMultipartFile

### Community 25 - ".sanitize"
Cohesion: 0.23
Nodes (4): org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, StoredFilename, StoredFilenameTest

### Community 26 - "CorsConfiguration"
Cohesion: 0.24
Nodes (7): org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.web.servlet.config.annotation.CorsRegistry, org.springframework.web.servlet.config.annotation.WebMvcConfigurer, CorsConfiguration, Override, NvidiaApiConfiguration

### Community 27 - "DocumentProcessingService.java"
Cohesion: 0.07
Nodes (23): org.springframework.web.multipart.MultipartFile, PostMapping, RequestMapping, RestController, DocumentController, ExplainedField, ExplainedMapping, ProcessExplanation (+15 more)

### Community 47 - "org.springframework.stereotype.Component"
Cohesion: 0.19
Nodes (5): org.springframework.stereotype.Component, LayoutCell, LayoutRow, CellOrigin, RegionClassifier

### Community 48 - "LayoutRegion"
Cohesion: 0.26
Nodes (4): java.util.regex.Pattern, ContinuationCandidate, LayoutRegion, RegionContinuationDetector

### Community 52 - "LayoutAnalyzerTest.java"
Cohesion: 0.24
Nodes (5): RegionKind, KEY_VALUE, LIST, PROSE, TABLE

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService.java` to `DocumentProcessingException`, `ExcelService`, `ExcelColumn`, `org.junit.jupiter.api.Test`, `DocumentElement`, `LayoutRecordMapper`, `DocumentProcessingBenchmarkTest`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `DocumentProcessingException`, `ExcelColumn`, `DocumentLayout`, `org.junit.jupiter.api.Test`, `LayoutRecordMapper`, `org.springframework.stereotype.Component`, `BBox`, `.analyze`, `LayoutAnalyzerTest.java`, `TableCellSplitter`, `DocumentProcessingService.java`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Why does `BBox` connect `BBox` to `DocumentProcessingException`, `ExcelColumn`, `DocumentLayout`, `org.junit.jupiter.api.Test`, `DocumentElement`, `LayoutRecordMapper`, `org.springframework.stereotype.Component`, `LayoutRegion`, `.analyze`, `LayoutAnalyzerTest.java`, `DocumentProcessingService.java`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.template()`) actually correct?**
  _`ExcelColumn` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DocumentProcessingException` be split into smaller, more focused modules?**
  _Cohesion score 0.066167290886392 - nodes in this community are weakly interconnected._