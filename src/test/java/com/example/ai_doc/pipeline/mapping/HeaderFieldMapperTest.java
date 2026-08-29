package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderFieldMapperTest {

    private final HeaderNameNormalizer normalizer = new HeaderNameNormalizer();
    private final HeaderFieldMapper mapper = new HeaderFieldMapper(normalizer);

    @Test
    void normalizeTrimsCollapsesWhitespaceAndIgnoresCase() {
        assertThat(normalizer.normalize("  TemPeRaTuRe\t Reading  "))
                .isEqualTo("temperature reading");
    }

    @Test
    void mapToColumnsUsesNormalizedExactMatchingAndWritesDuplicateHeaders() {
        ExcelTemplateInfo templateInfo = new ExcelTemplateInfo(
                "Sheet1",
                0,
                1,
                List.of(
                        new ExcelColumn(0, "Pressure"),
                        new ExcelColumn(2, "Temperature"),
                        new ExcelColumn(4, "Pressure")
                ));
        ExtractedDocumentData extractedData = new ExtractedDocumentData(List.of(
                new ExtractedField(" pressure ", "125 PSI"),
                new ExtractedField("TEMPERATURE", "80 C"),
                new ExtractedField("Material", "Steel")
        ));

        Map<Integer, String> mappedValues = mapper.mapToColumns(templateInfo, extractedData);

        assertThat(mappedValues)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        0, "125 PSI",
                        2, "80 C",
                        4, "125 PSI"
                ));
    }
}
