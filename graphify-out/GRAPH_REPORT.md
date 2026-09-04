# Graph Report - ai-doc  (2026-09-04)

## Corpus Check
- 130 files · ~49,820 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 794 nodes · 2545 edges · 52 communities (45 shown, 7 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 288 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a9ea02a0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NemotronDocumentUnderstandingService
- DocumentProcessingService
- org.springframework.http.ResponseEntity
- org.junit.jupiter.api.Test
- .analyze
- Document
- What You Must Do When Invoked
- ExcelColumn
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
- DocumentProcessingService.java
- .sanitize
- ExcelWriteModeTest
- DocumentProcessingMultiRowTest.java
- LayoutRow
- LayoutRegion
- BBox
- NoTemplateMode
- SemanticMappingValidationTest

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
- `ExtractedDocumentData` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/document/ExtractedDocumentData.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `DocumentMapping` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `ExcelTemplateInfo` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/excel/ExcelTemplateInfo.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `CarriedHeaderBand` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/pipeline/mapping/LayoutRecordMapper.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `DocumentElement` --references--> `BBox`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/layout/DocumentElement.java → src/main/java/com/example/ai_doc/domain/layout/BBox.java

## Import Cycles
- None detected.

## Communities (52 total, 7 thin omitted)

### Community 0 - "NemotronDocumentUnderstandingService"
Cohesion: 0.06
Nodes (26): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, org.springframework.web.servlet.config.annotation.CorsRegistry (+18 more)

### Community 1 - "DocumentProcessingService"
Cohesion: 0.06
Nodes (27): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Workbook, org.springframework.web.multipart.MultipartFile, PDFRenderer, PostMapping, RequestMapping, RestController (+19 more)

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.14
Nodes (14): org.junit.jupiter.api.BeforeEach, org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException (+6 more)

### Community 3 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (12): org.junit.jupiter.api.Test, ExtractedDocumentData, ProcessedExcelFile, HeaderFieldMapper, HeaderNameNormalizer, ExcelTemplateValidator, MockMultipartFile, PipelineMicroBenchmark (+4 more)

### Community 4 - ".analyze"
Cohesion: 0.11
Nodes (4): LayoutHeaderInferrer, LayoutAnalyzerTest, LayoutHeaderInferrerTest, LayoutRecordMapperTest

### Community 5 - "Document"
Cohesion: 0.05
Nodes (15): Entity, org.springframework.beans.factory.annotation.Autowired, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, EmptyFileException, FileSizeExceededException, InvalidFileTypeException, Document (+7 more)

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "ExcelColumn"
Cohesion: 0.09
Nodes (16): ExplainedField, ExplainedMapping, ProcessExplanation, ExternalAiServiceException, ExtractedField, ExcelColumn, DeterministicMappingResult, IndexedExtractedField (+8 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.11
Nodes (4): DocumentElement, ColumnAssignment, Geometry, TableCellSplitter

### Community 11 - "LayoutRecordMapper"
Cohesion: 0.14
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

### Community 24 - "DocumentProcessingService.java"
Cohesion: 0.27
Nodes (8): org.springframework.stereotype.Component, ColumnClusterer, LayoutAnalyzer, RegionClassifier, RowBander, VerticalSlabSplitter, HeaderInferenceService, ParsedDocumentFlattener

### Community 25 - ".sanitize"
Cohesion: 0.23
Nodes (4): org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, StoredFilename, StoredFilenameTest

### Community 26 - "ExcelWriteModeTest"
Cohesion: 0.41
Nodes (4): org.apache.poi.ss.usermodel.Sheet, org.apache.poi.xssf.usermodel.XSSFWorkbook, ExcelWriteModeTest, XSSFWorkbook

### Community 27 - "DocumentProcessingMultiRowTest.java"
Cohesion: 0.10
Nodes (15): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.junit.jupiter.api.condition.EnabledIfSystemProperty, org.springframework.boot.test.context.SpringBootTest, org.springframework.mock.web.MockMultipartFile, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, PageGeometry, ParsedDocument (+7 more)

### Community 47 - "LayoutRow"
Cohesion: 0.22
Nodes (3): java.util.regex.Pattern, LayoutCell, LayoutRow

### Community 48 - "LayoutRegion"
Cohesion: 0.16
Nodes (9): ContinuationCandidate, DocumentLayout, LayoutRegion, RegionKind, KEY_VALUE, LIST, PROSE, TABLE (+1 more)

### Community 49 - "BBox"
Cohesion: 0.17
Nodes (3): BBox, CellOrigin, ColumnGutterDetector

### Community 50 - "NoTemplateMode"
Cohesion: 0.67
Nodes (3): NoTemplateMode, INFERRED_HEADERS, RAW_FIELDS

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `NemotronDocumentUnderstandingService`, `org.springframework.http.ResponseEntity`, `org.junit.jupiter.api.Test`, `Document`, `ExcelColumn`, `LayoutRecordMapper`, `NoTemplateMode`, `DocumentProcessingService.java`, `DocumentProcessingMultiRowTest.java`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `NemotronDocumentUnderstandingService`, `.analyze`, `LayoutRow`, `LayoutRegion`, `BBox`, `DocumentProcessingService.java`, `DocumentProcessingMultiRowTest.java`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `DocumentProcessingService` to `NemotronDocumentUnderstandingService`, `org.springframework.http.ResponseEntity`, `org.junit.jupiter.api.Test`, `Document`, `ExcelColumn`, `LayoutRow`, `DocumentProcessingService.java`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.template()`) actually correct?**
  _`ExcelColumn` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `NemotronDocumentUnderstandingService` be split into smaller, more focused modules?**
  _Cohesion score 0.06128364389233954 - nodes in this community are weakly interconnected._