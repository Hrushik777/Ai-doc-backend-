# Graph Report - ai-doc  (2026-09-10)

## Corpus Check
- 143 files · ~55,968 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 914 nodes · 2904 edges · 60 communities (49 shown, 11 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 305 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `10e3269e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ExcelColumn
- ExcelTemplateInfo
- org.springframework.http.ResponseEntity
- ExtractedField
- ExtractedDocumentData
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
- DocumentProcessingService.java
- .sanitize
- DocumentProcessingService
- DocumentProcessingBenchmarkTest
- LayoutRow
- LayoutRegion
- HeaderNameNormalizer
- .analyze
- DocumentProcessingServiceTest.java
- .validate
- LayoutHeaderInferrer
- .mapLayout
- PipelineMicroBenchmark
- org.springframework.beans.factory.annotation.Autowired
- SecurityConfiguration.java
- DocumentFileValidator
- EmptyFileException

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 57 edges
2. `DocumentProcessingService` - 51 edges
3. `BBox` - 45 edges
4. `ExtractedDocumentData` - 40 edges
5. `ExcelColumn` - 39 edges
6. `ExcelTemplateInfo` - 36 edges
7. `LayoutRegion` - 34 edges
8. `ExcelService` - 31 edges
9. `DocumentProcessingException` - 29 edges
10. `DocumentLayout` - 28 edges

## Surprising Connections (you probably didn't know these)
- `ProcessExplanation` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `ExtractedDocumentData` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/document/ExtractedDocumentData.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `IndexedExtractedField` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/mapping/IndexedExtractedField.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `DocumentMapping` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `ExcelTemplateInfo` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/excel/ExcelTemplateInfo.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java

## Import Cycles
- None detected.

## Communities (60 total, 11 thin omitted)

### Community 0 - "ExcelColumn"
Cohesion: 0.06
Nodes (26): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, PDFRenderer, DocumentProcessingException, ExternalAiServiceException (+18 more)

### Community 1 - "ExcelTemplateInfo"
Cohesion: 0.09
Nodes (20): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Sheet, org.apache.poi.ss.usermodel.Workbook, org.apache.poi.xssf.usermodel.XSSFWorkbook, InvalidExcelTemplateException, ExcelTemplateInfo, ExcelWriteMode (+12 more)

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.16
Nodes (12): org.junit.jupiter.api.BeforeEach, org.springframework.http.ResponseEntity, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException, ApiErrorResponse (+4 more)

### Community 3 - "ExtractedField"
Cohesion: 0.15
Nodes (8): org.junit.jupiter.api.condition.EnabledIfSystemProperty, ExtractedField, DeterministicMappingResult, MappingSource, DETERMINISTIC, SEMANTIC, STRUCTURAL, ResolvedFieldMapping

### Community 4 - "ExtractedDocumentData"
Cohesion: 0.21
Nodes (4): ExtractedDocumentData, HeaderFieldMapper, DeterministicMappingTest, HeaderFieldMapperTest

### Community 5 - "Document"
Cohesion: 0.12
Nodes (3): Entity, Document, Table

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (25): javax.crypto.SecretKey, org.junit.jupiter.api.Test, org.springframework.boot.test.context.SpringBootTest, org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc, org.springframework.security.oauth2.core.OAuth2TokenValidator, org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.oauth2.jwt.JwtClaimsSet, org.springframework.security.oauth2.jwt.JwtEncoder (+17 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.10
Nodes (5): BBox, DocumentElement, ColumnGutterDetector, Geometry, TableCellSplitter

### Community 11 - "LayoutRecordMapper"
Cohesion: 0.11
Nodes (7): MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading, RawFieldRecordBuilder, RawFieldRecordBuilderTest

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
Cohesion: 0.36
Nodes (7): org.springframework.stereotype.Component, ColumnClusterer, LayoutAnalyzer, RegionClassifier, RowBander, VerticalSlabSplitter, ParsedDocumentFlattener

### Community 25 - ".sanitize"
Cohesion: 0.21
Nodes (4): org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, StoredFilename, StoredFilenameTest

### Community 26 - "DocumentProcessingService"
Cohesion: 0.07
Nodes (21): org.springframework.web.multipart.MultipartFile, DocumentController, PostMapping, RequestMapping, RestController, ExplainedField, ExplainedMapping, ProcessExplanation (+13 more)

### Community 27 - "DocumentProcessingBenchmarkTest"
Cohesion: 0.30
Nodes (3): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, DocumentProcessingBenchmarkTest, MockMultipartFile

### Community 47 - "LayoutRow"
Cohesion: 0.16
Nodes (5): java.util.regex.Pattern, LayoutCell, LayoutRow, ColumnAssignment, ModelJsonResponses

### Community 48 - "LayoutRegion"
Cohesion: 0.15
Nodes (10): ContinuationCandidate, DocumentLayout, LayoutRegion, RegionKind, KEY_VALUE, LIST, PROSE, TABLE (+2 more)

### Community 49 - "HeaderNameNormalizer"
Cohesion: 0.43
Nodes (4): HeaderNameNormalizer, ExcelTemplateValidator, DocumentProcessingServiceTest, MockMultipartFile

### Community 51 - "DocumentProcessingServiceTest.java"
Cohesion: 0.19
Nodes (7): org.springframework.mock.web.MockMultipartFile, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, UnsupportedDocumentUnderstandingException, ProcessedExcelFile, SemanticMappingService, DocumentUnderstandingService

### Community 52 - ".validate"
Cohesion: 0.19
Nodes (3): InvalidFileTypeException, DocumentFileValidatorTest, TestFiles

### Community 53 - "LayoutHeaderInferrer"
Cohesion: 0.12
Nodes (5): HeaderInferenceService, LayoutHeaderInferrer, Override, NemotronHeaderInferenceService, LayoutHeaderInferrerTest

### Community 57 - "SecurityConfiguration.java"
Cohesion: 0.14
Nodes (15): HttpServletResponse, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.http.HttpStatus, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.oauth2.jwt.JwtDecoder, org.springframework.security.web.access.AccessDeniedHandler, org.springframework.security.web.AuthenticationEntryPoint (+7 more)

### Community 58 - "DocumentFileValidator"
Cohesion: 0.36
Nodes (5): org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, DocumentRepository, DocumentService, DocumentFileValidator

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `ExcelColumn`, `ExcelTemplateInfo`, `org.springframework.http.ResponseEntity`, `ExtractedField`, `ExtractedDocumentData`, `LayoutRecordMapper`, `DocumentProcessingServiceTest.java`, `LayoutHeaderInferrer`, `DocumentProcessingService.java`, `DocumentFileValidator`, `DocumentProcessingBenchmarkTest`?**
  _High betweenness centrality (0.053) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `ExcelColumn`, `ExtractedField`, `LayoutRow`, `LayoutRegion`, `.analyze`, `DocumentProcessingServiceTest.java`, `LayoutHeaderInferrer`, `.mapLayout`, `DocumentProcessingService.java`, `DocumentProcessingService`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `ExcelColumn` to `ExcelTemplateInfo`, `org.springframework.http.ResponseEntity`, `DocumentFileValidator`, `LayoutRow`, `HeaderNameNormalizer`, `DocumentProcessingServiceTest.java`, `LayoutHeaderInferrer`, `DocumentProcessingService.java`, `.sanitize`, `DocumentProcessingService`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Are the 16 inferred relationships involving `ExtractedDocumentData` (e.g. with `.coordinatesAProviderResultIntoTheMatchingTemplateCells()` and `.keepsDeterministicValuesWhenTheSemanticStageFails()`) actually correct?**
  _`ExtractedDocumentData` has 16 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ExcelColumn` be split into smaller, more focused modules?**
  _Cohesion score 0.06464776632302406 - nodes in this community are weakly interconnected._