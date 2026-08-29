package com.example.ai_doc.benchmark;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.mapping.DeterministicMappingResult;
import com.example.ai_doc.domain.mapping.SemanticMapping;
import com.example.ai_doc.domain.result.BatchProcessedExcelFile;
import com.example.ai_doc.domain.result.ProcessedExcelFile;
import com.example.ai_doc.pipeline.mapping.HeaderFieldMapper;
import com.example.ai_doc.pipeline.mapping.SemanticMappingService;
import com.example.ai_doc.pipeline.DocumentProcessingService;
import com.example.ai_doc.pipeline.understanding.DocumentUnderstandingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
                // Database is not important for this benchmark.
                "spring.datasource.url=jdbc:h2:mem:aidoc-benchmark;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",

                // NVIDIA configuration.
                "nvidia.api.base-url=https://integrate.api.nvidia.com/v1",
                "nvidia.api.connect-timeout=10s",
                "nvidia.api.read-timeout=60s",
                "nvidia.parse.model=nvidia/nemotron-parse",
                "nvidia.mapping.model=nvidia/nemotron-3-super-120b-a12b",

                // Make sure the LLM actually returns mappings.
                "app.mapping.llm.confidence-threshold=0.80"
        }
)
@EnabledIfSystemProperty(named = "benchmark", matches = "true",
        disabledReason = "Benchmarks are opt-in: run with -Dbenchmark=true")
@EnabledIfEnvironmentVariable(named = "NVIDIA_API_KEY", matches = ".+",
        disabledReason = "This benchmark makes real NVIDIA calls and needs NVIDIA_API_KEY")
class DocumentProcessingBenchmarkTest {

    private static final String DOCUMENT_RESOURCE =
            "/benchmark/ai_doc_test_document.pdf";

    private static final String TEMPLATE_RESOURCE =
            "/benchmark/ai_doc_test_template.xlsx";

    private static final int WARMUP_RUNS = 2;
    private static final int BENCHMARK_RUNS = 10;
    private static final int BATCH_DOCUMENT_COUNT = 10;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @MockitoSpyBean
    private SemanticMappingService semanticMappingService;

    @MockitoSpyBean
    private DocumentUnderstandingService documentUnderstandingService;

    @MockitoSpyBean
    private HeaderFieldMapper headerFieldMapper;

    /**
     * Reads the real NVIDIA API key from the environment.
     *
     * Set NVIDIA_API_KEY in your IntelliJ test Run Configuration.
     */
    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {

        String apiKey = System.getenv("NVIDIA_API_KEY");
        registry.add("nvidia.api.key", () -> apiKey == null ? "" : apiKey);
    }

    @Test
    void benchmarkDocumentProcessing() {

        MockMultipartFile document = loadMultipartFile(
                DOCUMENT_RESOURCE,
                "document",
                "ai_doc_test_document.pdf",
                "application/pdf"
        );

        MockMultipartFile template = loadMultipartFile(
                TEMPLATE_RESOURCE,
                "template",
                "ai_doc_test_template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        System.out.println();
        System.out.println("======================================================");
        System.out.println("           AI DOCUMENT BENCHMARK");
        System.out.println("======================================================");
        System.out.println("Warm-up runs:    " + WARMUP_RUNS);
        System.out.println("Benchmark runs:  " + BENCHMARK_RUNS);
        System.out.println("Document:        " + document.getOriginalFilename());
        System.out.println("Template:        " + template.getOriginalFilename());
        System.out.println("======================================================");

        /*
         * ------------------------------------------------------------
         * WARM-UP
         * ------------------------------------------------------------
         *
         * These are real executions but are NOT included in the results.
         */
        System.out.println();
        System.out.println("Starting warm-up...");

        for (int i = 1; i <= WARMUP_RUNS; i++) {

            long start = System.nanoTime();

            ProcessedExcelFile result =
                    documentProcessingService.process(
                            document,
                            template
                    );

            long elapsedMs = elapsedMillis(start);

            assertValidResult(result);

            System.out.println(
                    "Warm-up " + i + " -> " + elapsedMs + " ms"
            );
        }

        /*
         * ------------------------------------------------------------
         * ACTUAL BENCHMARK
         * ------------------------------------------------------------
         */
        System.out.println();
        System.out.println("Starting benchmark...");

        List<Long> timings = new ArrayList<>(BENCHMARK_RUNS);

        for (int i = 1; i <= BENCHMARK_RUNS; i++) {

            long start = System.nanoTime();

            ProcessedExcelFile result =
                    documentProcessingService.process(
                            document,
                            template
                    );

            long elapsedMs = elapsedMillis(start);

            assertValidResult(result);

            timings.add(elapsedMs);

            System.out.println(
                    "Run " + i
                            + " -> "
                            + elapsedMs
                            + " ms"
                            + " | output="
                            + result.content().length
                            + " bytes"
            );
        }

        /*
         * ------------------------------------------------------------
         * RESULTS
         * ------------------------------------------------------------
         */
        printResults(timings);
    }

    @Test
    void benchmarkBatchDocumentProcessing() {

        MockMultipartFile document = loadMultipartFile(
                DOCUMENT_RESOURCE,
                "documents",
                "ai_doc_test_document.pdf",
                "application/pdf"
        );

        MockMultipartFile template = loadMultipartFile(
                TEMPLATE_RESOURCE,
                "template",
                "ai_doc_test_template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        // Reuse the single benchmark PDF as all documents in the batch, same as the
        // single-document benchmark above reuses one file across its timed runs.
        List<MultipartFile> documents = Collections.<MultipartFile>nCopies(BATCH_DOCUMENT_COUNT, document);

        System.out.println();
        System.out.println("======================================================");
        System.out.println("           AI DOCUMENT BATCH BENCHMARK");
        System.out.println("======================================================");
        System.out.println("Batch size:      " + BATCH_DOCUMENT_COUNT);
        System.out.println("Document:        " + document.getOriginalFilename());
        System.out.println("Template:        " + template.getOriginalFilename());
        System.out.println("======================================================");

        /*
         * ------------------------------------------------------------
         * WARM-UP
         * ------------------------------------------------------------
         *
         * A small real batch call, NOT included in the results.
         */
        System.out.println();
        System.out.println("Starting warm-up...");

        documentProcessingService.processBatch(Collections.nCopies(2, document), template);
        Mockito.clearInvocations(semanticMappingService, documentUnderstandingService, headerFieldMapper);

        /*
         * ------------------------------------------------------------
         * PER-DOCUMENT PIPELINE METRICS
         * ------------------------------------------------------------
         *
         * Wrap the same real collaborator beans processBatch() already calls
         * (documentUnderstandingService -> headerFieldMapper -> semanticMappingService,
         * strictly in that order, once per document, since processing is sequential) to
         * record extracted/matched/unmatched/semantic-call counts per document without
         * changing production code.
         */
        List<Integer> extractedFieldCounts = new ArrayList<>();
        List<Integer> deterministicMatchCounts = new ArrayList<>();
        List<Integer> unmatchedFieldCounts = new ArrayList<>();
        List<Integer> semanticCallCounts = new ArrayList<>();

        Mockito.doAnswer(invocation -> {
            ExtractedDocumentData extracted = (ExtractedDocumentData) invocation.callRealMethod();
            extractedFieldCounts.add(extracted.fields().size());
            return extracted;
        }).when(documentUnderstandingService).extractFields(any());

        Mockito.doAnswer(invocation -> {
            DeterministicMappingResult matched = (DeterministicMappingResult) invocation.callRealMethod();
            deterministicMatchCounts.add(matched.mappingsByColumn().size());
            unmatchedFieldCounts.add(matched.unmatchedFields().size());
            // Seed this document's semantic-call count at 0; the semantic mapping answer
            // below increments the most recent entry only if it is actually invoked.
            semanticCallCounts.add(0);
            return matched;
        }).when(headerFieldMapper).findExactMatches(any(), any());

        Mockito.doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<SemanticMapping> mappings = (List<SemanticMapping>) invocation.callRealMethod();
            int lastIndex = semanticCallCounts.size() - 1;
            semanticCallCounts.set(lastIndex, semanticCallCounts.get(lastIndex) + 1);
            return mappings;
        }).when(semanticMappingService).mapUnmatchedFields(any(), any());

        /*
         * ------------------------------------------------------------
         * ACTUAL BENCHMARK
         * ------------------------------------------------------------
         */
        System.out.println();
        System.out.println("Starting batch benchmark...");

        long start = System.nanoTime();

        BatchProcessedExcelFile result =
                documentProcessingService.processBatch(
                        documents,
                        template
                );

        long totalMs = elapsedMillis(start);

        assertNotNull(result, "Batch processing returned null result");
        assertNotNull(result.content(), "Generated workbook is null");
        assertFalse(result.content().length == 0, "Generated workbook is empty");

        long failedCount = result.results().stream().filter(item -> !item.success()).count();
        int semanticLlmCalls = semanticCallCounts.stream().mapToInt(Integer::intValue).sum();

        System.out.println(
                "Batch call -> " + totalMs + " ms"
                        + " | documents=" + BATCH_DOCUMENT_COUNT
                        + " | failed=" + failedCount
                        + " | output=" + result.content().length + " bytes"
        );

        /*
         * ------------------------------------------------------------
         * RESULTS
         * ------------------------------------------------------------
         */
        printPerDocumentMetrics(
                extractedFieldCounts, deterministicMatchCounts, unmatchedFieldCounts, semanticCallCounts);
        printBatchResults(totalMs, BATCH_DOCUMENT_COUNT, failedCount, semanticLlmCalls);
    }

    private void printPerDocumentMetrics(List<Integer> extractedFieldCounts,
                                         List<Integer> deterministicMatchCounts,
                                         List<Integer> unmatchedFieldCounts,
                                         List<Integer> semanticCallCounts) {

        System.out.println();
        System.out.println("======================================================");
        System.out.println("            PER-DOCUMENT PIPELINE METRICS");
        System.out.println("======================================================");
        System.out.printf(
                "%-10s %-16s %-16s %-12s %-14s%n",
                "Document", "Extracted", "Deterministic", "Unmatched", "Semantic calls"
        );

        for (int i = 0; i < extractedFieldCounts.size(); i++) {
            System.out.printf(
                    "%-10d %-16d %-16d %-12d %-14d%n",
                    i + 1,
                    extractedFieldCounts.get(i),
                    deterministicMatchCounts.get(i),
                    unmatchedFieldCounts.get(i),
                    semanticCallCounts.get(i)
            );
        }

        System.out.println("======================================================");
    }

    private void printBatchResults(long totalMs, int documentCount, long failedCount, int semanticLlmCalls) {

        double averagePerDocumentMs = totalMs / (double) documentCount;
        double throughput = 60_000.0 / averagePerDocumentMs;

        System.out.println();
        System.out.println("======================================================");
        System.out.println("               BATCH BENCHMARK RESULTS");
        System.out.println("======================================================");

        System.out.println("Documents:            " + documentCount);
        System.out.println("Failed documents:     " + failedCount);
        System.out.println("Total time:           " + totalMs + " ms");

        System.out.printf(
                "Average per document: %.2f ms%n",
                averagePerDocumentMs
        );

        System.out.printf(
                "Throughput:           %.2f documents/min%n",
                throughput
        );

        System.out.println("Semantic LLM calls:   " + semanticLlmCalls);
        System.out.println("======================================================");
    }

    private void printResults(List<Long> timings) {

        List<Long> sorted = new ArrayList<>(timings);
        Collections.sort(sorted);

        long total = sorted.stream()
                .mapToLong(Long::longValue)
                .sum();

        double average =
                total / (double) sorted.size();

        long minimum =
                sorted.get(0);

        long maximum =
                sorted.get(sorted.size() - 1);

        double median =
                percentile(sorted, 50);

        double p95 =
                percentile(sorted, 95);

        double throughput =
                60_000.0 / average;

        System.out.println();
        System.out.println("======================================================");
        System.out.println("                  BENCHMARK RESULTS");
        System.out.println("======================================================");

        System.out.println(
                "Runs:             " + sorted.size()
        );

        System.out.printf(
                "Average:          %.2f ms%n",
                average
        );

        System.out.println(
                "Minimum:          " + minimum + " ms"
        );

        System.out.println(
                "Maximum:          " + maximum + " ms"
        );

        System.out.printf(
                "Median (p50):     %.2f ms%n",
                median
        );

        System.out.printf(
                "p95:              %.2f ms%n",
                p95
        );

        System.out.printf(
                "Throughput:       %.2f documents/min%n",
                throughput
        );

        System.out.println("======================================================");

        System.out.println();
        System.out.println("Raw timings:");
        System.out.println(sorted);
    }

    private double percentile(
            List<Long> sorted,
            double percentile) {

        if (sorted.isEmpty()) {
            return 0;
        }

        double index =
                (percentile / 100.0) * (sorted.size() - 1);

        int lower =
                (int) Math.floor(index);

        int upper =
                (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double weight =
                index - lower;

        return sorted.get(lower)
                + weight * (
                sorted.get(upper)
                        - sorted.get(lower)
        );
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void assertValidResult(ProcessedExcelFile result) {

        assertNotNull(
                result,
                "Processing returned null result"
        );

        assertNotNull(
                result.content(),
                "Generated workbook is null"
        );

        assertFalse(
                result.content().length == 0,
                "Generated workbook is empty"
        );
    }

    private MockMultipartFile loadMultipartFile(
            String resourcePath,
            String formName,
            String originalFilename,
            String contentType) {

        try (InputStream inputStream =
                     getClass().getResourceAsStream(resourcePath)) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Benchmark resource not found: "
                                + resourcePath
                );
            }

            return new MockMultipartFile(
                    formName,
                    originalFilename,
                    contentType,
                    inputStream
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load benchmark resource: "
                            + resourcePath,
                    exception
            );
        }
    }
}