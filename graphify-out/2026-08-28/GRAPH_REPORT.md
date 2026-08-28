# Graph Report - ai-doc  (2026-08-28)

## Corpus Check
- 98 files · ~40,297 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 622 nodes · 2000 edges · 26 communities (17 shown, 9 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 263 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `973b569a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DocumentProcessingException
- DocumentProcessingService.java
- org.springframework.http.ResponseEntity
- ExcelColumn
- DocumentProcessingBenchmarkTest.java
- Document
- What You Must Do When Invoked
- org.junit.jupiter.api.Test
- graphify reference: extra exports and benchmark
- mvnw
- DocumentElement
- DocumentLayout
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
- LayoutRecordMapper
- TableCellSplitter

## God Nodes (most connected - your core abstractions)
1. `DocumentElement` - 53 edges
2. `DocumentProcessingService` - 43 edges
3. `BBox` - 39 edges
4. `ExcelColumn` - 33 edges
5. `LayoutRegion` - 32 edges
6. `ExtractedDocumentData` - 29 edges
7. `ExcelTemplateInfo` - 29 edges
8. `DocumentProcessingException` - 28 edges
9. `DocumentLayout` - 28 edges
10. `LayoutAnalyzer` - 25 edges

## Surprising Connections (you probably didn't know these)
- `DocumentProcessingBenchmarkTest` --references--> `HeaderFieldMapper`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/mapping/HeaderFieldMapper.java
- `DocumentProcessingBenchmarkTest` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/processing/DocumentProcessingService.java
- `DocumentProcessingBenchmarkTest` --references--> `DocumentUnderstandingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/DocumentProcessingBenchmarkTest.java → src/main/java/com/example/ai_doc/service/understanding/DocumentUnderstandingService.java
- `PipelineMicroBenchmark` --references--> `ExcelService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/PipelineMicroBenchmark.java → src/main/java/com/example/ai_doc/service/excel/ExcelService.java
- `DocumentController` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/controller/DocumentController.java → src/main/java/com/example/ai_doc/service/processing/DocumentProcessingService.java

## Import Cycles
- None detected.

## Communities (26 total, 9 thin omitted)

### Community 0 - "DocumentProcessingException"
Cohesion: 0.06
Nodes (26): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.beans.factory.annotation.Autowired, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.stereotype.Service, org.springframework.web.client.RestClient (+18 more)

### Community 1 - "DocumentProcessingService.java"
Cohesion: 0.06
Nodes (23): org.apache.poi.ss.usermodel.Workbook, org.springframework.web.multipart.MultipartFile, PostMapping, EmptyFileException, FileSizeExceededException, InvalidExcelTemplateException, InvalidFileTypeException, NoExcelMappingsException (+15 more)

### Community 2 - "org.springframework.http.ResponseEntity"
Cohesion: 0.10
Nodes (17): CrossOrigin, org.junit.jupiter.api.BeforeEach, org.springframework.http.ResponseEntity, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, RequestMapping (+9 more)

### Community 3 - "ExcelColumn"
Cohesion: 0.10
Nodes (18): org.apache.poi.ss.usermodel.Row, org.apache.poi.xssf.usermodel.XSSFWorkbook, org.springframework.mock.web.MockMultipartFile, MockMultipartFile, PipelineMicroBenchmark, ExtractedDocumentData, ExtractedField, ExcelColumn (+10 more)

### Community 4 - "DocumentProcessingBenchmarkTest.java"
Cohesion: 0.15
Nodes (8): org.springframework.boot.test.context.SpringBootTest, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, DocumentProcessingBenchmarkTest, MockMultipartFile, DeterministicMappingResult, SemanticMappingService, AiDocApplicationTests

### Community 5 - "Document"
Cohesion: 0.11
Nodes (6): Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, Document, DocumentRepository, Table

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - "org.junit.jupiter.api.Test"
Cohesion: 0.17
Nodes (10): FunctionalInterface, org.junit.jupiter.api.Test, IndexedExtractedField, SemanticMapping, Override, ExcelServiceTest, MockMultipartFile, WorkbookCustomizer (+2 more)

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "DocumentElement"
Cohesion: 0.06
Nodes (24): java.util.regex.Pattern, org.springframework.stereotype.Component, BBox, ContinuationCandidate, DocumentElement, LayoutCell, LayoutRegion, LayoutRow (+16 more)

### Community 11 - "DocumentLayout"
Cohesion: 0.10
Nodes (6): DocumentLayout, HeaderInferenceService, LayoutHeaderInferrer, LayoutAnalyzerTest, LayoutHeaderInferrerTest, LayoutRecordMapperTest

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

## Knowledge Gaps
- **57 isolated node(s):** `com.example:ai-doc`, `TABLE`, `KEY_VALUE`, `LIST`, `PROSE` (+52 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentElement` connect `DocumentElement` to `DocumentProcessingException`, `DocumentProcessingService.java`, `ExcelColumn`, `DocumentLayout`, `TableCellSplitter`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `DocumentProcessingService` connect `DocumentProcessingService.java` to `DocumentProcessingException`, `org.springframework.http.ResponseEntity`, `ExcelColumn`, `DocumentProcessingBenchmarkTest.java`, `DocumentElement`, `DocumentLayout`, `LayoutRecordMapper`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Why does `DocumentProcessingException` connect `DocumentProcessingException` to `DocumentProcessingService.java`, `org.springframework.http.ResponseEntity`, `ExcelColumn`, `org.junit.jupiter.api.Test`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `BBox` (e.g. with `.stack()` and `.toDocumentElement()`) actually correct?**
  _`BBox` has 8 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.mapToColumnsUsesNormalizedExactMatchingAndWritesDuplicateHeaders()`) actually correct?**
  _`ExcelColumn` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `TABLE`, `KEY_VALUE` to the rest of the system?**
  _57 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DocumentProcessingException` be split into smaller, more focused modules?**
  _Cohesion score 0.0645045045045045 - nodes in this community are weakly interconnected._