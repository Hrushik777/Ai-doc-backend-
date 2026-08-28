package com.example.ai_doc.benchmark;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.excel.ExcelColumn;
import com.example.ai_doc.model.excel.ExcelTemplateInfo;
import com.example.ai_doc.service.excel.ExcelService;
import com.example.ai_doc.service.mapping.HeaderFieldMapper;
import com.example.ai_doc.service.mapping.HeaderNameNormalizer;
import com.example.ai_doc.service.validation.ExcelTemplateValidator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Network-free micro benchmark for the CPU-bound pipeline stages (normalization,
 * deterministic mapping, Excel read/write). Deliberately excludes every LLM call so the
 * numbers are reproducible and comparable across runs.
 */
class PipelineMicroBenchmark {

    private static final int NORMALIZE_ITERATIONS = 2_000_000;
    private static final int MAPPING_ITERATIONS = 200_000;
    private static final int EXCEL_ITERATIONS = 2_000;

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

        // ---- normalize ----
        long start = System.nanoTime();
        for (int i = 0; i < NORMALIZE_ITERATIONS; i++) {
            normalizer.normalize("  Maximum   Allowable Working Pressure ");
        }
        long normalizeNs = System.nanoTime() - start;

        // ---- deterministic mapping (fields x headers) ----
        start = System.nanoTime();
        for (int i = 0; i < MAPPING_ITERATIONS; i++) {
            headerFieldMapper.findExactMatches(templateInfo, extractedData);
        }
        long mappingNs = System.nanoTime() - start;

        // ---- excel open + write row + serialize ----
        start = System.nanoTime();
        for (int i = 0; i < EXCEL_ITERATIONS; i++) {
            excelRoundTrip(template);
        }
        long excelNs = System.nanoTime() - start;

        System.out.println();
        System.out.println("======================================================");
        System.out.println("            PIPELINE MICRO BENCHMARK");
        System.out.println("======================================================");
        System.out.printf("normalize()        %,d iterations -> %,.0f ns/op%n",
                NORMALIZE_ITERATIONS, normalizeNs / (double) NORMALIZE_ITERATIONS);
        System.out.printf("findExactMatches() %,d iterations -> %,.0f ns/op  (%d fields x %d headers)%n",
                MAPPING_ITERATIONS, mappingNs / (double) MAPPING_ITERATIONS,
                extractedData.fields().size(), templateInfo.headers().size());
        System.out.printf("excel round trip   %,d iterations -> %,.0f ns/op%n",
                EXCEL_ITERATIONS, excelNs / (double) EXCEL_ITERATIONS);
        System.out.println("======================================================");
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
