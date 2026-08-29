package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.document.ExtractedDocumentData;
import com.example.ai_doc.domain.document.ExtractedField;
import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.domain.mapping.DeterministicMappingResult;
import com.example.ai_doc.domain.mapping.IndexedExtractedField;
import com.example.ai_doc.domain.mapping.ResolvedFieldMapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge cases of the exact-match stage: the one that must never guess.
 *
 * <p>Everything this stage produces is written at confidence 1.0 and outranks anything the
 * model later proposes, so a wrong match here is not corrected downstream - it silently
 * becomes the answer.
 */
class DeterministicMappingTest {

    private final HeaderFieldMapper mapper =
            new HeaderFieldMapper(new HeaderNameNormalizer(), new HeaderAliases());

    /**
     * Canonicalization has to apply to headers as well as to fields. Only the first two
     * rows of this worked before: a template header spelled with the abbreviation matched
     * nothing, because the index was keyed on the header's raw spelling.
     */
    @Test
    void aliasMatchingIsSymmetric() {
        assertThat(matches("Manufacturer", "Mfr")).isTrue();
        assertThat(matches("Manufacturer", "Manufacturer")).isTrue();
        assertThat(matches("Mfr", "Mfr")).isTrue();
        assertThat(matches("Mfr", "Manufacturer")).isTrue();

        assertThat(matches("MAWP", "MAWP")).isTrue();
        assertThat(matches("MAWP", "Maximum Allowable Working Pressure")).isTrue();
        assertThat(matches("Maximum Allowable Working Pressure", "MAWP")).isTrue();
    }

    @Test
    void aliasesCanBeReplacedOrDisabledByConfiguration() {
        HeaderFieldMapper noAliases = new HeaderFieldMapper(
                new HeaderNameNormalizer(), new HeaderAliases(HeaderAliases.NONE));
        assertThat(matchedColumns(noAliases, List.of("Manufacturer"), "Mfr")).isEmpty();

        HeaderFieldMapper custom = new HeaderFieldMapper(
                new HeaderNameNormalizer(), new HeaderAliases("qty=quantity;amt=amount"));
        assertThat(matchedColumns(custom, List.of("Quantity"), "Qty")).containsExactly(0);
        assertThat(matchedColumns(custom, List.of("Manufacturer"), "Mfr")).isEmpty();
    }

    @Test
    void casingWhitespaceAndTrailingColonAllNormalizeToTheSameColumn() {
        assertThat(matches("Temperature Reading", "  TemPeRaTuRe\t Reading  ")).isTrue();
        assertThat(matches("Tag Number", "Tag Number:")).isTrue();
        assertThat(matches("Tag Number:", "Tag Number")).isTrue();
    }

    /** Duplicate header names are legal, and every column carrying the name must be filled. */
    @Test
    void everyColumnSharingAHeaderNameReceivesTheValue() {
        assertThat(matchedColumns(mapper, List.of("Pressure", "Other", "Pressure"), "Pressure"))
                .containsExactly(0, 2);
    }

    /** Two fields competing for one column: document order decides, deterministically. */
    @Test
    void whenTwoFieldsClaimOneColumnTheFirstInDocumentOrderWins() {
        DeterministicMappingResult result = mapper.findExactMatches(
                template("Pressure"),
                new ExtractedDocumentData(List.of(
                        new ExtractedField("Pressure", "first"),
                        new ExtractedField("Pressure", "second"))));

        assertThat(result.mappingsByColumn().get(0).value()).isEqualTo("first");
    }

    @Test
    void fieldsWithNoUsableValueOrNameAreNeverMapped() {
        DeterministicMappingResult result = mapper.findExactMatches(
                template("Pressure", "Temperature"),
                new ExtractedDocumentData(List.of(
                        new ExtractedField("Pressure", null),
                        new ExtractedField("   ", "orphan value"),
                        new ExtractedField("Temperature", "80 C"))));

        assertThat(result.mappingsByColumn()).containsOnlyKeys(1);
        assertThat(result.mappingsByColumn().get(1).value()).isEqualTo("80 C");
    }

    /**
     * The guarantee that matters most: a value reaches exactly the column its own field
     * named, and no other. A leak here writes one reading into another reading's cell.
     */
    @Test
    void aValueNeverReachesAColumnOtherThanItsOwn() {
        DeterministicMappingResult result = mapper.findExactMatches(
                template("Pressure", "Temperature", "Manufacturer"),
                new ExtractedDocumentData(List.of(
                        new ExtractedField("Temperature", "80 C"),
                        new ExtractedField("Mfr", "Siemens"),
                        new ExtractedField("Pressure", "125 PSI"))));

        Map<Integer, ResolvedFieldMapping> byColumn = result.mappingsByColumn();
        assertThat(byColumn.get(0).value()).isEqualTo("125 PSI");
        assertThat(byColumn.get(1).value()).isEqualTo("80 C");
        assertThat(byColumn.get(2).value()).isEqualTo("Siemens");
    }

    /** Only what did not match may reach the model - that gating is what bounds the cost. */
    @Test
    void unmatchedFieldsAreTheOnlyOnesHandedOn() {
        DeterministicMappingResult result = mapper.findExactMatches(
                template("Pressure"),
                new ExtractedDocumentData(List.of(
                        new ExtractedField("Pressure", "125 PSI"),
                        new ExtractedField("Some prose heading", "unrelated narrative"))));

        assertThat(result.unmatchedFields())
                .extracting(IndexedExtractedField::fieldIndex)
                .containsExactly(1);
    }

    // ------------------------------------------------------------------------- helpers

    private boolean matches(String headerName, String fieldName) {
        return !matchedColumns(mapper, List.of(headerName), fieldName).isEmpty();
    }

    private List<Integer> matchedColumns(HeaderFieldMapper target,
                                         List<String> headerNames,
                                         String fieldName) {
        DeterministicMappingResult result = target.findExactMatches(
                template(headerNames.toArray(new String[0])),
                new ExtractedDocumentData(List.of(new ExtractedField(fieldName, "VALUE"))));
        return new ArrayList<>(result.mappingsByColumn().keySet());
    }

    private ExcelTemplateInfo template(String... headerNames) {
        List<ExcelColumn> headers = new ArrayList<>(headerNames.length);
        for (int i = 0; i < headerNames.length; i++) {
            headers.add(new ExcelColumn(i, headerNames[i]));
        }
        return new ExcelTemplateInfo("Sheet1", 0, 1, headers);
    }
}
