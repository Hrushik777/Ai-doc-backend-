# Graph Report - ai-doc  (2026-09-17)

## Corpus Check
- 149 files · ~59,275 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 971 nodes · 3127 edges · 56 communities (47 shown, 9 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 330 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cd5e853d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NemotronDocumentUnderstandingService
- DocumentProcessingService
- org.springframework.http.ResponseEntity
- ExtractedField
- DocumentProcessingService.java
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
- org.springframework.stereotype.Component
- .parse
- ExcelTemplateInfo
- DocumentProcessingBenchmarkTest
- LayoutRow
- LayoutRegion
- ExtractedDocumentData
- .analyze
- DocumentFileValidator
- DocumentLayout
- DocumentElement
- FreeAttemptAllowance
- DocumentProcessingServiceTest.java

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
- `AuthController` --references--> `SessionTokenIssuer`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/AuthController.java → src/main/java/com/example/ai_doc/auth/SessionTokenIssuer.java
- `DocumentController` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/DocumentController.java → src/main/java/com/example/ai_doc/pipeline/DocumentProcessingService.java
- `ProcessExplanation` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/api/dto/ProcessExplanation.java → src/main/java/com/example/ai_doc/domain/excel/ExcelColumn.java
- `ExtractedDocumentData` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/document/ExtractedDocumentData.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java
- `IndexedExtractedField` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/domain/mapping/IndexedExtractedField.java → src/main/java/com/example/ai_doc/domain/document/ExtractedField.java

## Import Cycles
- None detected.

## Communities (56 total, 9 thin omitted)

### Community 0 - "NemotronDocumentUnderstandingService"
Cohesion: 0.05
Nodes (28): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, PDFRenderer, ExternalAiServiceException, UnsupportedDocumentUnderstandingException (+20 more)

### Community 1 - "DocumentProcessingService"
Cohesion: 0.13
Nodes (8): org.springframework.web.multipart.MultipartFile, NoExcelMappingsException, ParsedDocument, DocumentMapping, DocumentOutcome, DocumentProcessingService, PreparedWorkbook, Override

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.07
Nodes (31): HttpServletResponse, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.oauth2.jwt.JwtDecoder, org.springframework.security.web.access.AccessDeniedHandler (+23 more)

### Community 3 - "ExtractedField"
Cohesion: 0.14
Nodes (7): ExtractedField, DeterministicMappingResult, MappingSource, DETERMINISTIC, SEMANTIC, STRUCTURAL, ResolvedFieldMapping

### Community 4 - "DocumentProcessingService.java"
Cohesion: 0.14
Nodes (11): org.springframework.beans.factory.annotation.Autowired, ExplainedField, ExplainedMapping, ProcessExplanation, NoTemplateMode, INFERRED_HEADERS, RAW_FIELDS, HeaderInferenceService (+3 more)

### Community 5 - "Document"
Cohesion: 0.07
Nodes (11): Entity, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.ValueSource, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, DocumentService (+3 more)

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (16): javax.crypto.SecretKey, org.junit.jupiter.api.Test, org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc, org.springframework.security.oauth2.core.OAuth2TokenValidator, org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.oauth2.jwt.JwtClaimsSet, org.springframework.security.oauth2.jwt.JwtEncoder, org.springframework.security.oauth2.jwt.NimbusJwtDecoder (+8 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 11 - "ExcelColumn"
Cohesion: 0.08
Nodes (10): ExcelColumn, CellOrigin, MappedRecord, CarriedHeaderBand, LayoutRecordMapper, RegionMapping, RegionReading, RawFieldRecordBuilder (+2 more)

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

### Community 25 - ".parse"
Cohesion: 0.23
Nodes (4): BatchConcurrencyTest, MockMultipartFile, DocumentProcessingMultiRowTest, MockMultipartFile

### Community 26 - "ExcelTemplateInfo"
Cohesion: 0.07
Nodes (24): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Sheet, org.apache.poi.ss.usermodel.Workbook, org.apache.poi.xssf.usermodel.XSSFWorkbook, org.junit.jupiter.api.condition.EnabledIfSystemProperty, DocumentProcessingException, InvalidExcelTemplateException (+16 more)

### Community 27 - "DocumentProcessingBenchmarkTest"
Cohesion: 0.22
Nodes (5): org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, DocumentProcessingBenchmarkTest, MockMultipartFile

### Community 47 - "LayoutRow"
Cohesion: 0.13
Nodes (9): java.util.regex.Pattern, ContinuationCandidate, LayoutCell, LayoutRow, RegionKind, KEY_VALUE, LIST, PROSE (+1 more)

### Community 49 - "ExtractedDocumentData"
Cohesion: 0.12
Nodes (9): ExtractedDocumentData, HeaderAliases, HeaderFieldMapper, HeaderNameNormalizer, ExcelTemplateValidator, DocumentProcessingServiceTest, MockMultipartFile, DeterministicMappingTest (+1 more)

### Community 52 - "DocumentFileValidator"
Cohesion: 0.11
Nodes (6): EmptyFileException, FileSizeExceededException, InvalidFileTypeException, DocumentFileValidator, DocumentFileValidatorTest, TestFiles

### Community 53 - "DocumentLayout"
Cohesion: 0.23
Nodes (3): DocumentLayout, LayoutHeaderInferrer, LayoutHeaderInferrerTest

### Community 54 - "DocumentElement"
Cohesion: 0.20
Nodes (3): DocumentElement, ColumnAssignment, Geometry

### Community 57 - "FreeAttemptAllowance"
Cohesion: 0.09
Nodes (18): jakarta.servlet.http.HttpServletRequest, org.springframework.security.authorization.AuthorizationManager, org.springframework.security.authorization.AuthorizationResult, org.springframework.security.core.Authentication, org.springframework.security.web.access.intercept.RequestAuthorizationContext, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.PostMapping, org.springframework.web.bind.annotation.RequestMapping (+10 more)

### Community 59 - "DocumentProcessingServiceTest.java"
Cohesion: 0.14
Nodes (11): org.junit.jupiter.api.BeforeEach, org.springframework.boot.test.context.SpringBootTest, org.springframework.mock.web.MockMultipartFile, org.springframework.test.web.servlet.MockMvc, BatchItemResult, BatchProcessedExcelFile, ProcessedExcelFile, AiDocApplicationTests (+3 more)

## Knowledge Gaps
- **63 isolated node(s):** `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY`, `OVERWRITE`, `TABLE` (+58 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `NemotronDocumentUnderstandingService`, `org.springframework.http.ResponseEntity`, `ExtractedField`, `DocumentProcessingService.java`, `DocumentProcessingBenchmarkTest`, `ExcelColumn`, `ExtractedDocumentData`, `DocumentFileValidator`, `org.springframework.stereotype.Component`, `.parse`, `ExcelTemplateInfo`, `DocumentProcessingServiceTest.java`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `NemotronDocumentUnderstandingService`, `DocumentProcessingService`, `DocumentProcessingService.java`, `BBox`, `ExcelColumn`, `LayoutRow`, `.analyze`, `DocumentLayout`, `org.springframework.stereotype.Component`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `ExcelTemplateInfo` to `NemotronDocumentUnderstandingService`, `DocumentProcessingService`, `org.springframework.http.ResponseEntity`, `DocumentProcessingService.java`, `Document`, `ExtractedDocumentData`, `DocumentProcessingServiceTest.java`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 12 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `FILL_THEN_APPEND`, `APPEND_ONLY` to the rest of the system?**
  _63 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `NemotronDocumentUnderstandingService` be split into smaller, more focused modules?**
  _Cohesion score 0.05334692490655794 - nodes in this community are weakly interconnected._
- **Should `DocumentProcessingService` be split into smaller, more focused modules?**
  _Cohesion score 0.13356562137049943 - nodes in this community are weakly interconnected._