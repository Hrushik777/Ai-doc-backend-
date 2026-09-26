# Graph Report - ai-doc  (2026-09-26)

## Corpus Check
- 149 files · ~59,469 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 970 nodes · 3122 edges · 73 communities (59 shown, 14 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 330 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5221d01c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NvidiaChatCompletionClient
- DocumentProcessingService
- org.springframework.http.ResponseEntity
- ExtractedField
- org.springframework.web.multipart.MultipartFile
- Document
- What You Must Do When Invoked
- org.junit.jupiter.api.Test
- graphify reference: extra exports and benchmark
- mvnw
- BBox
- ExcelColumn
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
- org.springframework.mock.web.MockMultipartFile
- ExcelTemplateInfo
- DocumentProcessingBenchmarkTest.java
- DocumentLayout
- LayoutRegion
- ExtractedDocumentData
- .analyze
- NemotronDocumentUnderstandingService
- DocumentProcessingServiceTest.java
- LayoutHeaderInferrer
- DocumentElement
- SemanticMapping
- SecurityConfiguration.java
- FreeAttemptAllowance
- IndexedExtractedField
- DocumentControllerTest.java
- SessionTokenTest
- SessionTokenIssuer
- .mapLayout
- GoogleTokenVerifier
- BatchConcurrencyTest
- NemotronParseConcurrencyTest
- DocumentProcessingException
- SignedInOrWithinFreeAllowance
- CorsConfiguration
- ExplainedMapping
- NoExcelMappingsException
- UnsupportedDocumentUnderstandingException
- NoTemplateMode

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 58 edges
2. `DocumentProcessingService` - 58 edges
3. `BBox` - 47 edges
4. `ExtractedDocumentData` - 40 edges
5. `ExcelColumn` - 39 edges
6. `ExcelTemplateInfo` - 38 edges
7. `LayoutRegion` - 34 edges
8. `ExcelService` - 32 edges
9. `DocumentProcessingException` - 30 edges
10. `DocumentLayout` - 28 edges

## Surprising Connections (you probably didn't know these)
- `AuthController` --references--> `GoogleTokenVerifier`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/AuthController.java → src/main/java/com/example/ai_doc/auth/GoogleTokenVerifier.java
- `DocumentController` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/DocumentController.java → src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java
- `HealthController` --references--> `FreeAttemptAllowance`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/HealthController.java → src/main/java/com/example/ai_doc/auth/FreeAttemptAllowance.java
- `ProcessExplanation` --references--> `ExplainedMapping`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/api/dto/ExplainedMapping.java
- `ProcessExplanation` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java

## Import Cycles
- None detected.

## Communities (73 total, 14 thin omitted)

### Community 0 - "NvidiaChatCompletionClient"
Cohesion: 0.17
Nodes (8): org.slf4j.Logger, org.springframework.web.client.RestClient, ExternalAiServiceException, Override, NemotronHeaderInferenceService, NvidiaChatCompletionClient, tools.jackson.databind.node.ObjectNode, tools.jackson.databind.ObjectMapper

### Community 1 - "DocumentProcessingService"
Cohesion: 0.19
Nodes (5): ParsedDocument, DocumentMapping, DocumentOutcome, DocumentProcessingService, PreparedWorkbook

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.17
Nodes (12): org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.HttpRequestMethodNotSupportedException, org.springframework.web.multipart.MaxUploadSizeExceededException, org.springframework.web.multipart.support.MissingServletRequestPartException, org.springframework.web.servlet.NoHandlerFoundException, org.springframework.web.servlet.resource.NoResourceFoundException (+4 more)

### Community 3 - "ExtractedField"
Cohesion: 0.16
Nodes (7): ExtractedField, DeterministicMappingResult, MappingSource, DETERMINISTIC, SEMANTIC, STRUCTURAL, ResolvedFieldMapping

### Community 4 - "org.springframework.web.multipart.MultipartFile"
Cohesion: 0.19
Nodes (7): org.springframework.web.multipart.MultipartFile, DocumentController, PostMapping, RequestMapping, RestController, ExplainedField, ProcessExplanation

### Community 5 - "Document"
Cohesion: 0.11
Nodes (7): Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, DocumentService, Table

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (16): org.junit.jupiter.api.Test, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, org.springframework.boot.test.context.SpringBootTest, org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc, org.springframework.test.web.servlet.MockMvc, ProcessedExcelFile, StoredFilename (+8 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "BBox"
Cohesion: 0.12
Nodes (3): BBox, CellOrigin, TableCellSplitter

### Community 11 - "ExcelColumn"
Cohesion: 0.11
Nodes (8): ExcelColumn, MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading, RawFieldRecordBuilder, RawFieldRecordBuilderTest

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
Cohesion: 0.28
Nodes (8): org.springframework.stereotype.Component, ColumnClusterer, ColumnGutterDetector, LayoutAnalyzer, RegionClassifier, RowBander, VerticalSlabSplitter, ParsedDocumentFlattener

### Community 25 - "org.springframework.mock.web.MockMultipartFile"
Cohesion: 0.41
Nodes (3): org.springframework.mock.web.MockMultipartFile, DocumentProcessingMultiRowTest, MockMultipartFile

### Community 26 - "ExcelTemplateInfo"
Cohesion: 0.07
Nodes (22): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Sheet, org.apache.poi.ss.usermodel.Workbook, org.apache.poi.xssf.usermodel.XSSFWorkbook, InvalidExcelTemplateException, ExcelTemplateInfo, ExcelWriteMode (+14 more)

### Community 27 - "DocumentProcessingBenchmarkTest.java"
Cohesion: 0.21
Nodes (6): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.junit.jupiter.api.condition.EnabledIfSystemProperty, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, DocumentProcessingBenchmarkTest, MockMultipartFile

### Community 47 - "DocumentLayout"
Cohesion: 0.13
Nodes (11): java.util.regex.Pattern, ContinuationCandidate, DocumentLayout, LayoutCell, LayoutRow, RegionKind, KEY_VALUE, LIST (+3 more)

### Community 49 - "ExtractedDocumentData"
Cohesion: 0.10
Nodes (10): org.springframework.beans.factory.annotation.Autowired, ExtractedDocumentData, HeaderAliases, HeaderFieldMapper, HeaderNameNormalizer, ExcelTemplateValidator, DocumentProcessingServiceTest, MockMultipartFile (+2 more)

### Community 51 - "NemotronDocumentUnderstandingService"
Cohesion: 0.22
Nodes (5): DocumentPageImage, Override, NemotronDocumentUnderstandingService, PageResult, tools.jackson.databind.JsonNode

### Community 52 - "DocumentProcessingServiceTest.java"
Cohesion: 0.11
Nodes (8): EmptyFileException, FileSizeExceededException, InvalidFileTypeException, SemanticMappingService, DocumentUnderstandingService, DocumentFileValidator, DocumentFileValidatorTest, TestFiles

### Community 54 - "DocumentElement"
Cohesion: 0.20
Nodes (3): DocumentElement, ColumnAssignment, Geometry

### Community 55 - "SemanticMapping"
Cohesion: 0.17
Nodes (5): org.springframework.stereotype.Service, SemanticMapping, SemanticMappingResponse, NemotronSemanticMappingService, ModelJsonResponses

### Community 56 - "SecurityConfiguration.java"
Cohesion: 0.20
Nodes (11): HttpServletResponse, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.http.HttpStatus, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.oauth2.jwt.JwtDecoder, org.springframework.security.web.access.AccessDeniedHandler, org.springframework.security.web.AuthenticationEntryPoint (+3 more)

### Community 57 - "FreeAttemptAllowance"
Cohesion: 0.22
Nodes (4): jakarta.servlet.http.HttpServletRequest, org.springframework.web.bind.annotation.GetMapping, ServiceStatus, FreeAttemptAllowance

### Community 58 - "IndexedExtractedField"
Cohesion: 0.39
Nodes (3): IndexedExtractedField, Override, NemotronSemanticMappingServiceTest

### Community 59 - "DocumentControllerTest.java"
Cohesion: 0.33
Nodes (4): org.junit.jupiter.api.BeforeEach, BatchItemResult, BatchProcessedExcelFile, DocumentControllerTest

### Community 60 - "SessionTokenTest"
Cohesion: 0.24
Nodes (4): javax.crypto.SecretKey, org.springframework.security.oauth2.jwt.JwtClaimsSet, org.springframework.security.oauth2.jwt.JwtEncoder, SessionTokenTest

### Community 61 - "SessionTokenIssuer"
Cohesion: 0.21
Nodes (9): org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController, AuthController, SessionRequest, SessionResponse, HealthController, IssuedSession (+1 more)

### Community 63 - "GoogleTokenVerifier"
Cohesion: 0.24
Nodes (5): org.springframework.security.oauth2.core.OAuth2TokenValidator, org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.oauth2.jwt.NimbusJwtDecoder, GoogleIdentity, GoogleTokenVerifier

### Community 65 - "NemotronParseConcurrencyTest"
Cohesion: 0.36
Nodes (3): FakePageRenderer, Override, NemotronParseConcurrencyTest

### Community 66 - "DocumentProcessingException"
Cohesion: 0.29
Nodes (5): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, PDFRenderer, DocumentProcessingException, PdfDocumentRenderer

### Community 67 - "SignedInOrWithinFreeAllowance"
Cohesion: 0.42
Nodes (6): org.springframework.security.authorization.AuthorizationManager, org.springframework.security.authorization.AuthorizationResult, org.springframework.security.core.Authentication, org.springframework.security.web.access.intercept.RequestAuthorizationContext, Override, SignedInOrWithinFreeAllowance

### Community 68 - "CorsConfiguration"
Cohesion: 0.38
Nodes (4): org.springframework.web.servlet.config.annotation.CorsRegistry, org.springframework.web.servlet.config.annotation.WebMvcConfigurer, CorsConfiguration, Override

### Community 72 - "NoTemplateMode"
Cohesion: 0.67
Nodes (3): NoTemplateMode, INFERRED_HEADERS, RAW_FIELDS

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `NvidiaChatCompletionClient`, `BatchConcurrencyTest`, `DocumentProcessingException`, `ExtractedField`, `org.springframework.web.multipart.MultipartFile`, `ExplainedMapping`, `org.junit.jupiter.api.Test`, `NoTemplateMode`, `DocumentProcessingBenchmarkTest.java`, `ExcelColumn`, `DocumentLayout`, `ExtractedDocumentData`, `DocumentProcessingServiceTest.java`, `SemanticMapping`, `DocumentProcessingService.java`, `org.springframework.mock.web.MockMultipartFile`, `ExcelTemplateInfo`, `DocumentControllerTest.java`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `NvidiaChatCompletionClient`, `DocumentProcessingService`, `BBox`, `DocumentLayout`, `.analyze`, `NemotronDocumentUnderstandingService`, `DocumentProcessingServiceTest.java`, `LayoutHeaderInferrer`, `DocumentProcessingService.java`, `.mapLayout`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `DocumentProcessingException` to `NvidiaChatCompletionClient`, `DocumentProcessingService`, `org.springframework.http.ResponseEntity`, `ExtractedField`, `org.springframework.web.multipart.MultipartFile`, `Document`, `DocumentLayout`, `ExtractedDocumentData`, `NemotronDocumentUnderstandingService`, `DocumentProcessingServiceTest.java`, `SemanticMapping`, `DocumentProcessingService.java`, `ExcelTemplateInfo`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 12 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Document` be split into smaller, more focused modules?**
  _Cohesion score 0.10869565217391304 - nodes in this community are weakly interconnected._
- **Should `What You Must Do When Invoked` be split into smaller, more focused modules?**
  _Cohesion score 0.07407407407407407 - nodes in this community are weakly interconnected._