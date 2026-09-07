# Graph Report - ai-doc  (2026-09-05)

## Corpus Check
- 131 files · ~51,621 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 816 nodes · 2630 edges · 59 communities (49 shown, 10 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 294 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `13efe287`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NemotronDocumentUnderstandingService
- ExcelTemplateInfo
- org.springframework.http.ResponseEntity
- ExtractedDocumentData
- .analyze
- Document
- What You Must Do When Invoked
- org.junit.jupiter.api.Test
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
- org.springframework.stereotype.Component
- .sanitize
- DocumentProcessingService.java
- DocumentProcessingBenchmarkTest.java
- DocumentLayout
- LayoutRegion
- BBox
- DocumentProcessingService
- DocumentProcessingException
- .mapLayout
- LayoutHeaderInferrer
- org.springframework.web.multipart.MultipartFile
- EmptyFileException
- ParsedDocument
- CorsConfiguration
- DocumentController

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 57 edges
2. `DocumentProcessingService` - 51 edges
3. `BBox` - 45 edges
4. `ExcelColumn` - 39 edges
5. `ExtractedDocumentData` - 37 edges
6. `ExcelTemplateInfo` - 36 edges
7. `LayoutRegion` - 34 edges
8. `ExcelService` - 31 edges
9. `DocumentProcessingException` - 29 edges
10. `DocumentLayout` - 28 edges

## Surprising Connections (you probably didn't know these)
- `DocumentController` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/DocumentController.java → src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java
- `ProcessExplanation` --references--> `ExplainedMapping`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/api/dto/ExplainedMapping.java
- `ProcessExplanation` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `IndexedExtractedField` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/mapping/IndexedExtractedField.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `DocumentMapping` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java

## Import Cycles
- None detected.

## Communities (59 total, 10 thin omitted)

### Community 0 - "NemotronDocumentUnderstandingService"
Cohesion: 0.07
Nodes (21): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, PDFRenderer, UnsupportedDocumentUnderstandingException, SemanticMappingResponse (+13 more)

### Community 1 - "ExcelTemplateInfo"
Cohesion: 0.09
Nodes (20): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Sheet, org.apache.poi.ss.usermodel.Workbook, org.apache.poi.xssf.usermodel.XSSFWorkbook, InvalidExcelTemplateException, ExcelTemplateInfo, ExcelWriteMode (+12 more)

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.17
Nodes (12): org.junit.jupiter.api.BeforeEach, org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException (+4 more)

### Community 3 - "ExtractedDocumentData"
Cohesion: 0.08
Nodes (17): org.springframework.beans.factory.annotation.Autowired, ExtractedDocumentData, ExtractedField, DeterministicMappingResult, MappingSource, DETERMINISTIC, SEMANTIC, STRUCTURAL (+9 more)

### Community 5 - "Document"
Cohesion: 0.12
Nodes (6): Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, Table

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (10): org.junit.jupiter.api.Test, ExternalAiServiceException, ExcelColumn, IndexedExtractedField, SemanticMapping, Override, NemotronSemanticMappingService, NemotronSemanticMappingServiceTest (+2 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.20
Nodes (3): DocumentElement, ColumnAssignment, Geometry

### Community 11 - "LayoutRecordMapper"
Cohesion: 0.15
Nodes (6): MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading, RawFieldRecordBuilder

### Community 12 - "AiDocApplication"
Cohesion: 0.48
Nodes (5): org.springframework.boot.autoconfigure.SpringBootApplication, org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration, org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration, org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration, AiDocApplication

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

### Community 24 - "org.springframework.stereotype.Component"
Cohesion: 0.42
Nodes (7): org.springframework.stereotype.Component, ColumnClusterer, ColumnGutterDetector, LayoutAnalyzer, RegionClassifier, RowBander, VerticalSlabSplitter

### Community 25 - ".sanitize"
Cohesion: 0.23
Nodes (4): org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, StoredFilename, StoredFilenameTest

### Community 26 - "DocumentProcessingService.java"
Cohesion: 0.12
Nodes (7): ExplainedField, ProcessExplanation, NoExcelMappingsException, BatchItemResult, BatchProcessedExcelFile, ProcessedExcelFile, ParsedDocumentFlattener

### Community 27 - "DocumentProcessingBenchmarkTest.java"
Cohesion: 0.08
Nodes (18): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.junit.jupiter.api.condition.EnabledIfSystemProperty, org.springframework.boot.test.context.SpringBootTest, org.springframework.mock.web.MockMultipartFile, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, NoTemplateMode, INFERRED_HEADERS (+10 more)

### Community 47 - "DocumentLayout"
Cohesion: 0.12
Nodes (10): ContinuationCandidate, DocumentLayout, LayoutCell, LayoutRow, PageGeometry, RegionKind, KEY_VALUE, LIST (+2 more)

### Community 48 - "LayoutRegion"
Cohesion: 0.26
Nodes (3): java.util.regex.Pattern, LayoutRegion, RegionContinuationDetector

### Community 49 - "BBox"
Cohesion: 0.12
Nodes (3): BBox, CellOrigin, TableCellSplitter

### Community 50 - "DocumentProcessingService"
Cohesion: 0.24
Nodes (4): ExplainedMapping, DocumentMapping, DocumentProcessingService, PreparedWorkbook

### Community 51 - "DocumentProcessingException"
Cohesion: 0.19
Nodes (4): DocumentProcessingException, DocumentService, DocumentFileValidator, DocumentFileValidatorTest

### Community 54 - "org.springframework.web.multipart.MultipartFile"
Cohesion: 0.21
Nodes (3): org.springframework.web.multipart.MultipartFile, FileSizeExceededException, Override

### Community 55 - "EmptyFileException"
Cohesion: 0.18
Nodes (3): EmptyFileException, InvalidFileTypeException, TestFiles

### Community 57 - "CorsConfiguration"
Cohesion: 0.24
Nodes (7): org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.web.servlet.config.annotation.CorsRegistry, org.springframework.web.servlet.config.annotation.WebMvcConfigurer, CorsConfiguration, Override, NvidiaApiConfiguration

### Community 58 - "DocumentController"
Cohesion: 0.33
Nodes (4): PostMapping, RequestMapping, RestController, DocumentController

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `NemotronDocumentUnderstandingService`, `ExcelTemplateInfo`, `org.springframework.http.ResponseEntity`, `ExtractedDocumentData`, `DocumentController`, `LayoutRecordMapper`, `DocumentProcessingException`, `org.springframework.web.multipart.MultipartFile`, `ParsedDocument`, `org.springframework.stereotype.Component`, `DocumentProcessingService.java`, `DocumentProcessingBenchmarkTest.java`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `NemotronDocumentUnderstandingService`, `ExtractedDocumentData`, `.analyze`, `DocumentLayout`, `BBox`, `.mapLayout`, `LayoutHeaderInferrer`, `ParsedDocument`, `org.springframework.stereotype.Component`, `DocumentProcessingService.java`?**
  _High betweenness centrality (0.051) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `DocumentProcessingException` to `NemotronDocumentUnderstandingService`, `ExcelTemplateInfo`, `org.springframework.http.ResponseEntity`, `ExtractedDocumentData`, `org.junit.jupiter.api.Test`, `DocumentProcessingService`, `org.springframework.web.multipart.MultipartFile`, `DocumentProcessingService.java`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.template()`) actually correct?**
  _`ExcelColumn` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `NemotronDocumentUnderstandingService` be split into smaller, more focused modules?**
  _Cohesion score 0.07115384615384615 - nodes in this community are weakly interconnected._