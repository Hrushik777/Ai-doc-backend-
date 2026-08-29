# Graph Report - ai-doc  (2026-08-29)

## Corpus Check
- 128 files · ~47,489 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 758 nodes · 2427 edges · 47 communities (40 shown, 7 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 298 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7fd5a85e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DocumentProcessingException
- ExcelService
- org.springframework.http.ResponseEntity
- ExcelColumn
- org.junit.jupiter.api.Test
- Document
- What You Must Do When Invoked
- .process
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

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 55 edges
2. `DocumentProcessingService` - 49 edges
3. `BBox` - 45 edges
4. `ExcelColumn` - 39 edges
5. `ExtractedDocumentData` - 37 edges
6. `LayoutRegion` - 34 edges
7. `ExcelTemplateInfo` - 31 edges
8. `DocumentLayout` - 28 edges
9. `DocumentProcessingException` - 27 edges
10. `LayoutRecordMapper` - 27 edges

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

## Communities (47 total, 7 thin omitted)

### Community 0 - "DocumentProcessingException"
Cohesion: 0.07
Nodes (25): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, PDFRenderer, DocumentProcessingException, ExternalAiServiceException (+17 more)

### Community 1 - "ExcelService"
Cohesion: 0.16
Nodes (8): FunctionalInterface, org.apache.poi.ss.usermodel.Workbook, InvalidExcelTemplateException, ExcelService, SynthesizedTemplate, ExcelServiceTest, MockMultipartFile, WorkbookCustomizer

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.28
Nodes (8): org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException, ApiErrorResponse, GlobalExceptionHandler

### Community 3 - "ExcelColumn"
Cohesion: 0.08
Nodes (16): org.apache.poi.ss.usermodel.Row, org.apache.poi.xssf.usermodel.XSSFWorkbook, org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.junit.jupiter.api.condition.EnabledIfSystemProperty, org.springframework.mock.web.MockMultipartFile, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, ExtractedDocumentData (+8 more)

### Community 4 - "org.junit.jupiter.api.Test"
Cohesion: 0.08
Nodes (11): org.junit.jupiter.api.Test, org.springframework.boot.test.context.SpringBootTest, DocumentLayout, PageGeometry, LayoutHeaderInferrer, Override, AiDocApplicationTests, LayoutAnalyzerTest (+3 more)

### Community 5 - "Document"
Cohesion: 0.05
Nodes (15): Entity, org.springframework.beans.factory.annotation.Autowired, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, EmptyFileException, FileSizeExceededException, InvalidFileTypeException, Document (+7 more)

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - ".process"
Cohesion: 0.20
Nodes (7): HeaderNameNormalizer, ExcelTemplateValidator, DocumentProcessingMultiRowTest, MockMultipartFile, DocumentProcessingServiceTest, MockMultipartFile, XSSFWorkbook

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.05
Nodes (23): java.util.regex.Pattern, org.springframework.stereotype.Component, BBox, ContinuationCandidate, DocumentElement, LayoutCell, LayoutRegion, LayoutRow (+15 more)

### Community 11 - "LayoutRecordMapper"
Cohesion: 0.17
Nodes (6): CellOrigin, MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading

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
Cohesion: 0.14
Nodes (7): org.junit.jupiter.api.BeforeEach, org.springframework.test.web.servlet.MockMvc, AIServiceNotConfiguredException, ProcessedExcelFile, DocumentControllerTest, DocumentProcessingBenchmarkTest, MockMultipartFile

### Community 25 - ".sanitize"
Cohesion: 0.23
Nodes (4): org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, StoredFilename, StoredFilenameTest

### Community 26 - "CorsConfiguration"
Cohesion: 0.24
Nodes (7): org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.web.servlet.config.annotation.CorsRegistry, org.springframework.web.servlet.config.annotation.WebMvcConfigurer, CorsConfiguration, Override, NvidiaApiConfiguration

### Community 27 - "DocumentProcessingService.java"
Cohesion: 0.06
Nodes (29): org.springframework.web.multipart.MultipartFile, PostMapping, RequestMapping, RestController, DocumentController, ExplainedField, ExplainedMapping, ProcessExplanation (+21 more)

## Knowledge Gaps
- **60 isolated node(s):** `com.example:ai-doc`, `TABLE`, `KEY_VALUE`, `LIST`, `PROSE` (+55 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService.java` to `DocumentProcessingException`, `ExcelService`, `ExcelColumn`, `Document`, `.process`, `DocumentElement`, `LayoutRecordMapper`, `DocumentProcessingBenchmarkTest`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `DocumentProcessingService.java`, `DocumentProcessingException`, `ExcelColumn`, `org.junit.jupiter.api.Test`?**
  _High betweenness centrality (0.052) - this node is a cross-community bridge._
- **Why does `BBox` connect `DocumentElement` to `DocumentProcessingException`, `ExcelColumn`, `org.junit.jupiter.api.Test`, `.process`, `LayoutRecordMapper`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.template()`) actually correct?**
  _`ExcelColumn` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `TABLE`, `KEY_VALUE` to the rest of the system?**
  _60 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DocumentProcessingException` be split into smaller, more focused modules?**
  _Cohesion score 0.06739811912225706 - nodes in this community are weakly interconnected._