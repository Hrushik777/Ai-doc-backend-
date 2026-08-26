package com.example.ai_doc.benchmark;

import com.example.ai_doc.model.processing.ProcessedExcelFile;
import com.example.ai_doc.service.processing.DocumentProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
                "nvidia.mapping.model=nvidia/nvidia-nemotron-nano-9b-v2",

                // Make sure the LLM actually returns mappings.
                "app.mapping.llm.confidence-threshold=0.80"
        }
)
class DocumentProcessingBenchmarkTest {

    private static final String DOCUMENT_RESOURCE =
            "/benchmark/ai_doc_test_document.pdf";

    private static final String TEMPLATE_RESOURCE =
            "/benchmark/ai_doc_test_template.xlsx";

    private static final int WARMUP_RUNS = 2;
    private static final int BENCHMARK_RUNS = 10;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    /**
     * Reads the real NVIDIA API key from the environment.
     *
     * Set NVIDIA_API_KEY in your IntelliJ test Run Configuration.
     */
    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {

        String apiKey = System.getenv("NVIDIA_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "NVIDIA_API_KEY environment variable is not set."
                            + " Add it to the IntelliJ Run Configuration environment variables."
            );
        }

        registry.add("nvidia.api.key", () -> apiKey);
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