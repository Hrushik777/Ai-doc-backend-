package com.example.ai_doc.benchmark;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.pipeline.excel.ExcelService;
import com.example.ai_doc.pipeline.mapping.HeaderFieldMapper;
import com.example.ai_doc.pipeline.mapping.HeaderNameNormalizer;
import com.example.ai_doc.pipeline.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Network-free micro benchmark for the CPU-bound pipeline stages (normalization,
 * deterministic mapping, Excel read/write). Deliberately excludes every LLM call so the
 * numbers are reproducible and comparable across runs.
 */
@EnabledIfSystemProperty(named = "benchmark", matches = "true",
        disabledReason = "Benchmarks are opt-in: run with -Dbenchmark=true")
class PipelineMicroBenchmark {

    private static final int NORMALIZE_ITERATIONS = 2_000_000;
    private static final int MAPPING_ITERATIONS = 200_000;
    private static final int EXCEL_ITERATIONS = 2_000;

    /** Repeats of the whole measurement, so run-to-run spread is visible. */
    private static final int TRIALS = 5;

    private final HeaderNameNormalizer normalizer = new HeaderNameNormalizer();
    private final HeaderFieldMapper headerFieldMapper = new HeaderFieldMapper(normalizer);
    private final ExcelService excelService = new ExcelService(new ExcelTemplateValidator(), 0);

    @Test
    void runMicroBenchmark() throws IOException {

        ExcelTemplateInfo templateInfo = templateInfo();
        ExtractedDocumentData extractedData = extractedData();
        MockMultipartFile template = xlsxTemplate();

        // ---- warm up (let the JIT compile everything) ----
        for (int i = 0; i < 50_000; i++) {
            normalizer.normalize("  Maximum   Allowable Working Pressure ");
        }
        for (int i = 0; i < 20_000; i++) {
            headerFieldMapper.findExactMatches(templateInfo, extractedData);
        }
        for (int i = 0; i < 200; i++) {
            excelRoundTrip(template);
        }

        // Each stage is measured several times over. A single timing cannot distinguish a
        // real change from the machine being busy, which is exactly the mistake this guards
        // against: on this hardware the untouched Excel path alone varies by ~20% run to run,
        // so any comparison that ignores spread will "prove" improvements that are not there.
        List<Double> normalizeNsPerOp = new ArrayList<>(TRIALS);
        List<Double> mappingNsPerOp = new ArrayList<>(TRIALS);
        List<Double> excelNsPerOp = new ArrayList<>(TRIALS);

        for (int trial = 0; trial < TRIALS; trial++) {
            long start = System.nanoTime();
            for (int i = 0; i < NORMALIZE_ITERATIONS; i++) {
                normalizer.normalize("  Maximum   Allowable Working Pressure ");
            }
            normalizeNsPerOp.add((System.nanoTime() - start) / (double) NORMALIZE_ITERATIONS);

            start = System.nanoTime();
            for (int i = 0; i < MAPPING_ITERATIONS; i++) {
                headerFieldMapper.findExactMatches(templateInfo, extractedData);
            }
            mappingNsPerOp.add((System.nanoTime() - start) / (double) MAPPING_ITERATIONS);

            start = System.nanoTime();
            for (int i = 0; i < EXCEL_ITERATIONS; i++) {
                excelRoundTrip(template);
            }
            excelNsPerOp.add((System.nanoTime() - start) / (double) EXCEL_ITERATIONS);
        }

        System.out.println();
        System.out.println("==================================================================");
        System.out.println("                    PIPELINE MICRO BENCHMARK");
        System.out.println("==================================================================");
        System.out.printf("Trials: %d   (per trial: normalize x%,d, mapping x%,d, excel x%,d)%n",
                TRIALS, NORMALIZE_ITERATIONS, MAPPING_ITERATIONS, EXCEL_ITERATIONS);
        System.out.printf("%-20s %10s %10s %10s %10s %10s %14s%n",
                "stage", "avg", "median", "p95", "min", "max", "throughput");
        System.out.println("------------------------------------------------------------------");
        report("normalize()", normalizeNsPerOp);
        report("findExactMatches()", mappingNsPerOp);
        report("excel round trip", excelNsPerOp);
        System.out.println("------------------------------------------------------------------");
        System.out.printf("mapping input: %d fields x %d headers%n",
                extractedData.fields().size(), templateInfo.headers().size());
        System.out.printf("spread (max/min): normalize %.2fx, mapping %.2fx, excel %.2fx%n",
                spread(normalizeNsPerOp), spread(mappingNsPerOp), spread(excelNsPerOp));
        System.out.println("A difference smaller than the spread is noise, not an improvement.");
        System.out.println("==================================================================");
    }

    /** One row of the results table, in nanoseconds per operation. */
    private void report(String stage, List<Double> nsPerOp) {
        List<Double> sorted = new ArrayList<>(nsPerOp);
        Collections.sort(sorted);

        double average = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        System.out.printf("%-20s %10s %10s %10s %10s %10s %10.0f/s%n",
                stage,
                format(average),
                format(percentile(sorted, 50)),
                format(percentile(sorted, 95)),
                format(sorted.get(0)),
                format(sorted.get(sorted.size() - 1)),
                average > 0 ? 1_000_000_000.0 / average : 0);
    }

    private String format(double nanos) {
        return String.format(java.util.Locale.ROOT, "%,.0f", nanos);
    }

    private double spread(List<Double> nsPerOp) {
        double min = nsPerOp.stream().mapToDouble(Double::doubleValue).min().orElse(1);
        double max = nsPerOp.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        return min > 0 ? max / min : 0;
    }

    private double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    private void excelRoundTrip(MockMultipartFile template) throws IOException {
        try (Workbook workbook = excelService.openWorkbook(template)) {
            ExcelTemplateInfo info = excelService.readHeaders(workbook);
            Map<Integer, String> values = new LinkedHashMap<>();
            values.put(0, "P-101");
            values.put(1, "Centrifugal Pump");
            values.put(2, "Siemens");
            excelService.writeRow(workbook, info, info.dataRowIndex(), values);
            byte[] out = excelService.serialize(workbook);
            if (out.length == 0) {
                throw new IllegalStateException("empty workbook");
            }
        }
    }

    private ExcelTemplateInfo templateInfo() {
        List<ExcelColumn> headers = List.of(
                new ExcelColumn(0, "Tag Number"),
                new ExcelColumn(1, "Equipment Type"),
                new ExcelColumn(2, "Manufacturer"),
                new ExcelColumn(3, "Maximum Allowable Working Pressure"),
                new ExcelColumn(4, "Design Pressure"),
                new ExcelColumn(5, "Design Temperature"),
                new ExcelColumn(6, "Serial Number"),
                new ExcelColumn(7, "Model"),
                new ExcelColumn(8, "Capacity"),
                new ExcelColumn(9, "Material"),
                new ExcelColumn(10, "Inspection Date"),
                new ExcelColumn(11, "Notes"));
        return new ExcelTemplateInfo("Measurements", 0, 1, headers);
    }

    private ExtractedDocumentData extractedData() {
        List<ExtractedField> fields = new ArrayList<>();
        fields.add(new ExtractedField("Tag Number", "P-101"));
        fields.add(new ExtractedField("Equipment Type", "Centrifugal Pump"));
        fields.add(new ExtractedField("Mfr", "Siemens"));
        fields.add(new ExtractedField("MAWP", "150 psi"));
        fields.add(new ExtractedField("Design Pressure", "120 psi"));
        // Realistic tail of prose/table noise that never matches a header.
        for (int i = 0; i < 35; i++) {
            fields.add(new ExtractedField("Unmatched Element " + i, "value " + i));
        }
        return new ExtractedDocumentData(fields);
    }

    private MockMultipartFile xlsxTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Measurements");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Tag Number");
            headerRow.createCell(1).setCellValue("Equipment Type");
            headerRow.createCell(2).setCellValue("Manufacturer");
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "template",
                    "template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
