# Graph Report - ai-doc  (2026-09-12)

## Corpus Check
- 144 files · ~57,620 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 938 nodes · 3031 edges · 65 communities (51 shown, 14 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 321 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cfca72a8`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NemotronDocumentUnderstandingService
- ExcelWriteModeTest
- org.springframework.http.ResponseEntity
- ExcelColumn
- DocumentProcessingService.java
- Document
- What You Must Do When Invoked
- org.junit.jupiter.api.Test
- graphify reference: extra exports and benchmark
- mvnw
- BBox
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
- org.springframework.mock.web.MockMultipartFile
- DocumentProcessingService
- DocumentProcessingBenchmarkTest
- LayoutRow
- LayoutRegion
- ExtractedDocumentData
- .analyze
- DocumentProcessingBenchmarkTest.java
- DocumentProcessingServiceTest.java
- DocumentLayout
- DocumentElement
- tools.jackson.databind.ObjectMapper
- HeaderFieldMapper
- SecurityConfiguration.java
- IndexedExtractedField
- DocumentControllerTest.java
- NemotronParseConcurrencyTest
- .parseMappingResponse
- PdfDocumentRenderer
- NemotronSemanticMappingService
- UnsupportedDocumentUnderstandingException

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 58 edges
2. `DocumentProcessingService` - 56 edges
3. `BBox` - 47 edges
4. `ExtractedDocumentData` - 40 edges
5. `ExcelColumn` - 39 edges
6. `ExcelTemplateInfo` - 38 edges
7. `LayoutRegion` - 34 edges
8. `ExcelService` - 32 edges
9. `DocumentProcessingException` - 30 edges
10. `DocumentLayout` - 28 edges

## Surprising Connections (you probably didn't know these)
- `ProcessExplanation` --references--> `ExplainedMapping`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/api/dto/ExplainedMapping.java
- `ProcessExplanation` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `SecurityFilterChainTest` --references--> `SessionTokenIssuer`  [EXTRACTED]
  src/test/java/com/example/ai_doc/api/SecurityFilterChainTest.java → src/main/java/com/example/ai_doc/auth/SessionTokenIssuer.java
- `ExtractedDocumentData` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/document/ExtractedDocumentData.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `IndexedExtractedField` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/mapping/IndexedExtractedField.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java

## Import Cycles
- None detected.

## Communities (65 total, 14 thin omitted)

### Community 0 - "NemotronDocumentUnderstandingService"
Cohesion: 0.18
Nodes (6): PDFRenderer, DocumentPageImage, Override, NemotronDocumentUnderstandingService, PageResult, tools.jackson.databind.JsonNode

### Community 1 - "ExcelWriteModeTest"
Cohesion: 0.27
Nodes (8): org.apache.poi.ss.usermodel.Sheet, org.apache.poi.xssf.usermodel.XSSFWorkbook, ExcelWriteMode, APPEND_ONLY, FILL_THEN_APPEND, OVERWRITE, ExcelWriteModeTest, XSSFWorkbook

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.20
Nodes (11): org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.HttpRequestMethodNotSupportedException, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException, org.springframework.web.servlet.NoHandlerFoundException, org.springframework.web.servlet.resource.NoResourceFoundException (+3 more)

### Community 4 - "DocumentProcessingService.java"
Cohesion: 0.12
Nodes (8): ExplainedField, ProcessExplanation, MappingSource, DETERMINISTIC, SEMANTIC, STRUCTURAL, ResolvedFieldMapping, HeaderInferenceService

### Community 5 - "Document"
Cohesion: 0.11
Nodes (7): Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, DocumentService, Table

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (13): org.junit.jupiter.api.Test, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, org.springframework.boot.test.context.SpringBootTest, org.springframework.security.oauth2.jwt.Jwt, StoredFilename, AiDocApplicationTests, MockMultipartFile (+5 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 11 - "LayoutRecordMapper"
Cohesion: 0.08
Nodes (9): CellOrigin, MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading, RawFieldRecordBuilder, LayoutRecordMapperTest (+1 more)

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
Cohesion: 0.38
Nodes (7): org.springframework.stereotype.Component, ColumnClusterer, ColumnGutterDetector, LayoutAnalyzer, RegionClassifier, RowBander, VerticalSlabSplitter

### Community 25 - "org.springframework.mock.web.MockMultipartFile"
Cohesion: 0.23
Nodes (5): org.springframework.mock.web.MockMultipartFile, BatchConcurrencyTest, MockMultipartFile, DocumentProcessingMultiRowTest, MockMultipartFile

### Community 26 - "DocumentProcessingService"
Cohesion: 0.05
Nodes (27): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Workbook, org.springframework.web.multipart.MultipartFile, DocumentController, PostMapping, RequestMapping, RestController (+19 more)

### Community 48 - "LayoutRegion"
Cohesion: 0.15
Nodes (9): java.util.regex.Pattern, ContinuationCandidate, LayoutRegion, RegionKind, KEY_VALUE, LIST, PROSE, TABLE (+1 more)

### Community 49 - "ExtractedDocumentData"
Cohesion: 0.12
Nodes (10): org.junit.jupiter.api.condition.EnabledIfSystemProperty, ExtractedDocumentData, HeaderNameNormalizer, ExcelTemplateValidator, MockMultipartFile, PipelineMicroBenchmark, DocumentProcessingServiceTest, MockMultipartFile (+2 more)

### Community 51 - "DocumentProcessingBenchmarkTest.java"
Cohesion: 0.28
Nodes (4): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, DeterministicMappingResult

### Community 52 - "DocumentProcessingServiceTest.java"
Cohesion: 0.20
Nodes (6): EmptyFileException, InvalidFileTypeException, SemanticMappingService, DocumentUnderstandingService, DocumentFileValidator, TestFiles

### Community 53 - "DocumentLayout"
Cohesion: 0.22
Nodes (4): DocumentLayout, LayoutHeaderInferrer, Override, LayoutHeaderInferrerTest

### Community 54 - "DocumentElement"
Cohesion: 0.19
Nodes (3): DocumentElement, ColumnAssignment, Geometry

### Community 55 - "tools.jackson.databind.ObjectMapper"
Cohesion: 0.20
Nodes (8): org.slf4j.Logger, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, ExternalAiServiceException, NemotronHeaderInferenceService, NvidiaChatCompletionClient, tools.jackson.databind.node.ObjectNode, tools.jackson.databind.ObjectMapper

### Community 56 - "HeaderFieldMapper"
Cohesion: 0.21
Nodes (6): org.springframework.beans.factory.annotation.Autowired, NoTemplateMode, INFERRED_HEADERS, RAW_FIELDS, HeaderAliases, HeaderFieldMapper

### Community 57 - "SecurityConfiguration.java"
Cohesion: 0.06
Nodes (32): HttpServletResponse, javax.crypto.SecretKey, org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.http.HttpStatus, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.oauth2.core.OAuth2TokenValidator (+24 more)

### Community 58 - "IndexedExtractedField"
Cohesion: 0.35
Nodes (4): IndexedExtractedField, SemanticMapping, Override, NemotronSemanticMappingServiceTest

### Community 59 - "DocumentControllerTest.java"
Cohesion: 0.20
Nodes (7): org.junit.jupiter.api.BeforeEach, org.springframework.test.web.servlet.MockMvc, AIServiceNotConfiguredException, BatchItemResult, BatchProcessedExcelFile, ProcessedExcelFile, DocumentControllerTest

### Community 60 - "NemotronParseConcurrencyTest"
Cohesion: 0.36
Nodes (3): FakePageRenderer, Override, NemotronParseConcurrencyTest

### Community 62 - "PdfDocumentRenderer"
Cohesion: 0.43
Nodes (3): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, PdfDocumentRenderer

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `ExcelWriteModeTest`, `DocumentProcessingService.java`, `DocumentProcessingBenchmarkTest`, `LayoutRecordMapper`, `DocumentProcessingBenchmarkTest.java`, `DocumentProcessingServiceTest.java`, `tools.jackson.databind.ObjectMapper`, `org.springframework.stereotype.Component`, `org.springframework.mock.web.MockMultipartFile`, `HeaderFieldMapper`, `DocumentControllerTest.java`, `PdfDocumentRenderer`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `NemotronDocumentUnderstandingService`, `BBox`, `LayoutRecordMapper`, `LayoutRow`, `LayoutRegion`, `.analyze`, `DocumentProcessingServiceTest.java`, `DocumentLayout`, `tools.jackson.databind.ObjectMapper`, `org.springframework.stereotype.Component`, `DocumentProcessingService`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `DocumentProcessingService` to `NemotronDocumentUnderstandingService`, `org.springframework.http.ResponseEntity`, `ExcelColumn`, `DocumentProcessingService.java`, `Document`, `LayoutRow`, `ExtractedDocumentData`, `DocumentProcessingServiceTest.java`, `tools.jackson.databind.ObjectMapper`, `.parseMappingResponse`, `PdfDocumentRenderer`?**
  _High betweenness centrality (0.040) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 12 INFERRED edges - model-reasoned connections that need verification._
- **Are the 16 inferred relationships involving `ExtractedDocumentData` (e.g. with `.coordinatesAProviderResultIntoTheMatchingTemplateCells()` and `.keepsDeterministicValuesWhenTheSemanticStageFails()`) actually correct?**
  _`ExtractedDocumentData` has 16 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DocumentProcessingService.java` be split into smaller, more focused modules?**
  _Cohesion score 0.12418300653594772 - nodes in this community are weakly interconnected._