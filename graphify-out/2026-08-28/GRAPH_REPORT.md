# Graph Report - ai-doc  (2026-08-28)

## Corpus Check
- 66 files · ~24,485 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 385 nodes · 1063 edges · 24 communities (17 shown, 7 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 148 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1252571d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DocumentProcessingException
- DocumentProcessingService.java
- .processBatch
- ExcelTemplateInfo
- org.junit.jupiter.api.Test
- org.springframework.web.multipart.MultipartFile
- What You Must Do When Invoked
- .validate
- graphify reference: extra exports and benchmark
- mvnw
- ExtractedDocumentData
- NvidiaApiConfiguration.java
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

## God Nodes (most connected - your core abstractions)
1. `DocumentProcessingException` - 25 edges
2. `ExtractedDocumentData` - 25 edges
3. `ExcelColumn` - 23 edges
4. `DocumentProcessingService` - 23 edges
5. `IndexedExtractedField` - 21 edges
6. `NemotronDocumentUnderstandingService` - 21 edges
7. `HeaderNameNormalizer` - 20 edges
8. `DocumentFileValidator` - 20 edges
9. `Document` - 19 edges
10. `SemanticMapping` - 18 edges

## Surprising Connections (you probably didn't know these)
- `PipelineMicroBenchmark` --references--> `HeaderFieldMapper`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/PipelineMicroBenchmark.java → src/main/java/com/example/ai_doc/service/mapping/HeaderFieldMapper.java
- `PipelineMicroBenchmark` --references--> `HeaderNameNormalizer`  [EXTRACTED]
  src/main/java/com/example/ai_doc/benchmark/PipelineMicroBenchmark.java → src/main/java/com/example/ai_doc/service/mapping/HeaderNameNormalizer.java
- `DocumentController` --references--> `DocumentProcessingService`  [EXTRACTED]
  src/main/java/com/example/ai_doc/controller/DocumentController.java → src/main/java/com/example/ai_doc/service/processing/DocumentProcessingService.java
- `ExtractedDocumentData` --references--> `ExtractedField`  [EXTRACTED]
  src/main/java/com/example/ai_doc/model/document/ExtractedDocumentData.java → src/main/java/com/example/ai_doc/model/document/ExtractedField.java
- `ExcelTemplateInfo` --references--> `ExcelColumn`  [EXTRACTED]
  src/main/java/com/example/ai_doc/model/excel/ExcelTemplateInfo.java → src/main/java/com/example/ai_doc/model/excel/ExcelColumn.java

## Import Cycles
- None detected.

## Communities (24 total, 7 thin omitted)

### Community 0 - "DocumentProcessingException"
Cohesion: 0.09
Nodes (20): java.awt.image.BufferedImage, org.apache.pdfbox.rendering.PDFRenderer, org.slf4j.Logger, org.springframework.stereotype.Component, org.springframework.stereotype.Service, org.springframework.web.client.RestClient, PDFRenderer, DocumentProcessingException (+12 more)

### Community 1 - "DocumentProcessingService.java"
Cohesion: 0.09
Nodes (15): org.springframework.boot.test.context.SpringBootTest, org.springframework.test.context.DynamicPropertyRegistry, org.springframework.test.context.DynamicPropertySource, DocumentProcessingBenchmarkTest, MockMultipartFile, DeterministicMappingResult, MappingSource, DETERMINISTIC (+7 more)

### Community 2 - ".processBatch"
Cohesion: 0.11
Nodes (13): org.junit.jupiter.api.BeforeEach, org.springframework.http.ResponseEntity, org.springframework.test.web.servlet.MockMvc, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.multipart.MaxUploadSizeExceededException, AIServiceNotConfiguredException, GlobalExceptionHandler (+5 more)

### Community 3 - "ExcelTemplateInfo"
Cohesion: 0.13
Nodes (13): FunctionalInterface, org.apache.poi.ss.usermodel.Row, org.apache.poi.ss.usermodel.Workbook, org.apache.poi.xssf.usermodel.XSSFWorkbook, org.springframework.mock.web.MockMultipartFile, MockMultipartFile, PipelineMicroBenchmark, InvalidExcelTemplateException (+5 more)

### Community 4 - "org.junit.jupiter.api.Test"
Cohesion: 0.17
Nodes (9): org.junit.jupiter.api.Test, ExternalAiServiceException, ExtractedField, ExcelColumn, IndexedExtractedField, SemanticMapping, Override, HeaderFieldMapperTest (+1 more)

### Community 5 - "org.springframework.web.multipart.MultipartFile"
Cohesion: 0.08
Nodes (13): CrossOrigin, Entity, org.springframework.data.jpa.repository.JpaRepository, org.springframework.stereotype.Repository, org.springframework.web.multipart.MultipartFile, PostMapping, RequestMapping, RestController (+5 more)

### Community 6 - "What You Must Do When Invoked"
Cohesion: 0.07
Nodes (26): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+18 more)

### Community 7 - ".validate"
Cohesion: 0.17
Nodes (4): EmptyFileException, FileSizeExceededException, InvalidFileTypeException, DocumentFileValidatorTest

### Community 8 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 9 - "mvnw"
Cohesion: 0.38
Nodes (8): mvnw script, clean(), die(), exec_maven(), hash_string(), set_java_home(), trim(), verbose()

### Community 10 - "ExtractedDocumentData"
Cohesion: 0.37
Nodes (8): java.util.regex.Pattern, ExtractedDocumentData, HeaderNameNormalizer, DocumentFileValidator, ExcelTemplateValidator, DocumentProcessingServiceTest, MockMultipartFile, XSSFWorkbook

### Community 11 - "NvidiaApiConfiguration.java"
Cohesion: 0.60
Nodes (3): org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, NvidiaApiConfiguration

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
- **53 isolated node(s):** `com.example:ai-doc`, `DETERMINISTIC`, `SEMANTIC`, `graphify`, `Usage` (+48 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DocumentProcessingException` connect `DocumentProcessingException` to `DocumentProcessingService.java`, `.processBatch`, `ExcelTemplateInfo`, `org.junit.jupiter.api.Test`, `org.springframework.web.multipart.MultipartFile`, `ExtractedDocumentData`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Why does `DocumentProcessingService` connect `DocumentProcessingService.java` to `DocumentProcessingException`, `.processBatch`, `ExcelTemplateInfo`, `org.springframework.web.multipart.MultipartFile`, `ExtractedDocumentData`?**
  _High betweenness centrality (0.043) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `ExtractedDocumentData` (e.g. with `.mapToColumnsUsesNormalizedExactMatchingAndWritesDuplicateHeaders()` and `.coordinatesAProviderResultIntoTheMatchingTemplateCells()`) actually correct?**
  _`ExtractedDocumentData` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `ExcelColumn` (e.g. with `.templateInfo()` and `.mapToColumnsUsesNormalizedExactMatchingAndWritesDuplicateHeaders()`) actually correct?**
  _`ExcelColumn` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.example:ai-doc`, `DETERMINISTIC`, `SEMANTIC` to the rest of the system?**
  _53 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DocumentProcessingException` be split into smaller, more focused modules?**
  _Cohesion score 0.09333333333333334 - nodes in this community are weakly interconnected._
- **Should `DocumentProcessingService.java` be split into smaller, more focused modules?**
  _Cohesion score 0.08599033816425121 - nodes in this community are weakly interconnected._