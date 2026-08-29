# Graph Report - ai-doc  (2026-08-28)

## Corpus Check
- 100 files · ~41,390 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 636 nodes · 2066 edges · 33 communities (24 shown, 9 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 274 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `973b569a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- NemotronDocumentUnderstandingService
- ExcelService
- org.springframework.http.ResponseEntity
- org.junit.jupiter.api.Test
- DocumentProcessingBenchmarkTest
- Document
- What You Must Do When Invoked
- .readHeaders
- graphify reference: extra exports and benchmark
- mvnw
- DocumentElement
- .analyze
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
- ExcelColumn
- DocumentProcessingService.java
- DocumentProcessingService
- org.springframework.web.multipart.MultipartFile
- .process
- .validate
- ParsedDocument
- MappingSource
- ExplainedField

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 53 edges
2. `DocumentProcessingService` - 46 edges
3. `BBox` - 42 edges
4. `ExcelColumn` - 33 edges
5. `LayoutRegion` - 32 edges
6. `ExtractedDocumentData` - 29 edges
7. `ExcelTemplateInfo` - 29 edges
8. `DocumentProcessingException` - 28 edges
9. `DocumentLayout` - 28 edges
10. `LayoutRow` - 25 edges

## Surprising Connections (you probably didn't know these)
- `DocumentProcessingBenchmarkTest` --references--> `HeaderFieldMapper`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/mapping/HeaderFieldMapper.java
- `DocumentProcessingBenchmarkTest` --references--> `SemanticMappingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/mapping/SemanticMappingService.java
- `DocumentProcessingBenchmarkTest` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/processing/DocumentProcessingService.java
- `DocumentProcessingBenchmarkTest` --references--> `DocumentUnderstandingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/understanding/DocumentUnderstandingService.java
- `PipelineMicroBenchmark` --references--> `ExcelService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/PipelineMicroBenchmark.java → src/main/java/com/example/ai_doc/service/excel/ExcelService.java

## Import Cycles
- None detected.

## Communities (33 total, 9 thin omitted)

### Community 0 - "NemotronDocumentUnderstandingService"
Cohesion: 0.07
Nodes (24): org.slf4j.Logger, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, NvidiaApiConfiguration, ExternalAiServiceException, UnsupportedDocumentUnderstandingException (+16 more)

### Community 1 - "ExcelService"
Cohesion: 0.21
Nodes (4): org.apache.poi.ss.usermodel.Workbook, FileSizeExceededException, InvalidExcelTemplateException, ExcelService

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.12
Nodes (12): org.junit.jupiter.api.BeforeEach, org.springframework.http.ResponseEntity, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, AIServiceNotConfiguredException, GlobalExceptionHandler (+4 more)

### Community 3 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (20): org.apache.poi.ss.usermodel.Row, org.apache.poi.xssf.usermodel.XSSFWorkbook, org.junit.jupiter.api.Test, org.springframework.mock.web.MockMultipartFile, MockMultipartFile, PipelineMicroBenchmark, ExtractedDocumentData, ExtractedField (+12 more)

### Community 4 - "DocumentProcessingBenchmarkTest"
Cohesion: 0.18
Nodes (6): org.springframework.boot.test.context.SpringBootTest, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, DocumentProcessingBenchmarkTest, MockMultipartFile, AiDocApplicationTests

### Community 5 - "Document"
Cohesion: 0.11
Nodes (6): Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, Table

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - ".readHeaders"
Cohesion: 0.29
Nodes (4): FunctionalInterface, ExcelServiceTest, MockMultipartFile, WorkbookCustomizer

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.05
Nodes (25): java.util.regex.Pattern, org.springframework.stereotype.Component, BBox, ContinuationCandidate, DocumentElement, DocumentLayout, LayoutCell, LayoutRegion (+17 more)

### Community 11 - ".analyze"
Cohesion: 0.13
Nodes (4): PageGeometry, LayoutHeaderInferrer, LayoutAnalyzerTest, LayoutHeaderInferrerTest

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

### Community 24 - "ExcelColumn"
Cohesion: 0.12
Nodes (7): ExcelColumn, ExcelTemplateInfo, MappedRecord, SynthesizedTemplate, LayoutRecordMapper, RegionMapping, LayoutRecordMapperTest

### Community 25 - "DocumentProcessingService.java"
Cohesion: 0.16
Nodes (8): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.springframework.beans.factory.annotation.Autowired, PDFRenderer, DocumentProcessingException, HeaderInferenceService, ParsedDocumentFlattener, PdfDocumentRenderer

### Community 26 - "DocumentProcessingService"
Cohesion: 0.20
Nodes (4): ExplainedMapping, DeterministicMappingResult, ResolvedFieldMapping, DocumentProcessingService

### Community 27 - "org.springframework.web.multipart.MultipartFile"
Cohesion: 0.23
Nodes (8): CrossOrigin, org.springframework.web.multipart.MultipartFile, PostMapping, RequestMapping, RestController, DocumentController, ProcessExplanation, DocumentService

### Community 28 - ".process"
Cohesion: 0.32
Nodes (3): NoExcelMappingsException, DocumentMapping, PreparedWorkbook

### Community 29 - ".validate"
Cohesion: 0.23
Nodes (3): EmptyFileException, InvalidFileTypeException, DocumentFileValidatorTest

### Community 31 - "MappingSource"
Cohesion: 0.40
Nodes (4): MappingSource, DETERMINISTIC, SEMANTIC, STRUCTURAL

## Knowledge Gaps
- **58 isolated node(s):** `com.example:ai-doc`, `TABLE`, `KEY_VALUE`, `LIST`, `PROSE` (+53 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingService` connect `DocumentProcessingService` to `NemotronDocumentUnderstandingService`, `ExcelService`, `org.springframework.http.ResponseEntity`, `org.junit.jupiter.api.Test`, `DocumentProcessingBenchmarkTest`, `ExplainedField`, `DocumentElement`, `ExcelColumn`, `DocumentProcessingService.java`, `org.springframework.web.multipart.MultipartFile`, `.process`, `ParsedDocument`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **Why does `DocumentElement` connect `DocumentElement` to `NemotronDocumentUnderstandingService`, `org.junit.jupiter.api.Test`, `.analyze`, `ExcelColumn`, `ParsedDocument`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Why does `BBox` connect `DocumentElement` to `NemotronDocumentUnderstandingService`, `ExcelColumn`, `.analyze`, `org.junit.jupiter.api.Test`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 8 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.mapToColumnsUsesNormalizedExactMatchingAndWritesDuplicateHeaders()`) actually correct?**
  _`ExcelColumn` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `TABLE`, `KEY_VALUE` to the rest of the system?**
  _58 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `NemotronDocumentUnderstandingService` be split into smaller, more focused modules?**
  _Cohesion score 0.06942053930005737 - nodes in this community are weakly interconnected._