package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.excel.ExcelTemplateInfo;
import com.example.ai_doc.domain.layout.ContinuationCandidate;
import com.example.ai_doc.domain.layout.DocumentLayout;
import com.example.ai_doc.domain.layout.LayoutCell;
import com.example.ai_doc.domain.layout.LayoutRegion;
import com.example.ai_doc.domain.layout.LayoutRow;
import com.example.ai_doc.domain.mapping.CellOrigin;
import com.example.ai_doc.domain.mapping.MappedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps an analyzed layout onto template columns, producing one record per document row.
 *
 * <p>Entirely deterministic: it matches a table's own header band against the template's
 * headers and then maps every following row positionally, so a twenty-row table costs no
 * model calls at all. Regions it cannot resolve are left alone, and the caller falls back
 * to the existing flat deterministic-then-semantic path for them.
 *
 * <p>Where a region has an alternative continued reading, <em>the template decides which one
 * is used</em>. Both readings are mapped and scored, and the better score wins. That is how
 * one two-column measurement list becomes six paired rows under a template of {@code L | R}
 * and twelve single readings under a template of {@code Index | Value}, without either
 * structure being written into the code.
 */
@Component
public class LayoutRecordMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(LayoutRecordMapper.class);

    /**
     * How many of a region's header cells must match template headers before the band is
     * believed to be a header row. One match is too easily a coincidence in a table of free
     * text; requiring two is what stops a data row being consumed as headers.
     */
    private static final int MIN_HEADER_MATCHES = 2;

    /** Positional mapping is a guess unless it explains several rows. */
    private static final int MIN_POSITIONAL_RECORDS = 2;

    /**
     * Separators that join several values into one cell, as {@code 1 - 3.5} does. Ordered
     * from most to least explicit so a value containing both is split on the stronger one.
     */
    private static final String[] COMPOSITE_SEPARATORS = {" - ", " – ", " — ", " = ", "\t", ": "};

    /** A named header match is worth far more than any amount of positional guessing. */
    private static final int HEADER_MATCH_WEIGHT = 1000;
    private static final int RECORD_WEIGHT = 10;
    private static final int ATOMIC_VALUE_BONUS = 5;

    private final HeaderFieldMapper headerFieldMapper;

    public LayoutRecordMapper(HeaderFieldMapper headerFieldMapper) {
        this.headerFieldMapper = headerFieldMapper;
    }

    /** Records produced for one region, and how well that reading is supported. */
    private record RegionMapping(List<MappedRecord> records, int score) {

        static RegionMapping none() {
            return new RegionMapping(List.of(), 0);
        }
    }

    /**
     * Returns one map of column to value per output row, or an empty list when the layout
     * offered nothing the template could take.
     */
    public List<MappedRecord> mapLayout(ExcelTemplateInfo templateInfo, DocumentLayout layout) {
        if (layout == null || layout.isEmpty()) {
            return List.of();
        }

        Map<String, List<ExcelColumn>> columnsByHeader =
                headerFieldMapper.indexHeadersByNormalizedName(templateInfo.headers());

        List<MappedRecord> tabularRecords = new ArrayList<>();
        Map<Integer, String> documentLevelValues = new LinkedHashMap<>();

        // A table continued onto the next page repeats its rows but not its header band, so
        // the mapping established on the first page is carried forward. Without this the
        // continuation resolves nothing, and a multi-page table silently loses every row
        // after the first page.
        CarriedHeaderBand carried = null;

        for (int index = 0; index < layout.regions().size(); index++) {
            LayoutRegion region = layout.regions().get(index);

            switch (region.kind()) {
                case TABLE, LIST -> {
                    RegionReading reading =
                            bestReading(layout, index, templateInfo, columnsByHeader, carried);
                    tabularRecords.addAll(reading.mapping().records());
                    if (reading.headerBand() != null) {
                        carried = reading.headerBand();
                    }
                }
                case KEY_VALUE -> documentLevelValues.putAll(mapKeyValueRegion(region, columnsByHeader));
                default -> {
                    // Prose carries no structure worth mapping positionally, and the semantic
                    // stage still sees it through the flat field path. One exception: the tail
                    // of a table continued across a page break has no header band of its own,
                    // and a short one classifies as prose for exactly that reason. A carried
                    // band of the same shape is enough evidence to map it - and it is the only
                    // evidence that would be, which is why nothing else here is mapped.
                    if (carried != null && carried.appliesTo(region)) {
                        LOGGER.debug("Unclassified region on page {} mapped as the continuation"
                                + " of the table whose header band was on page {}",
                                region.page(), carried.page());
                        tabularRecords.addAll(
                                mapRows(region, carried.columns(), 0, carried.columns().size()).records());
                    }
                }
            }
        }

        return combine(tabularRecords, documentLevelValues);
    }

    /**
     * Maps the region as it sits on the page and, where one exists, its continued reading,
     * and keeps whichever the template supports better. A tie keeps the page layout - the
     * arrangement actually printed - rather than guessing.
     */
    private RegionReading bestReading(DocumentLayout layout,
                                      int regionIndex,
                                      ExcelTemplateInfo templateInfo,
                                      Map<String, List<ExcelColumn>> columnsByHeader,
                                      CarriedHeaderBand carried) {

        LayoutRegion region = layout.regions().get(regionIndex);
        RegionReading best = mapRegion(region, templateInfo, columnsByHeader, carried);

        for (ContinuationCandidate candidate : layout.continuations()) {
            if (!candidate.regionIndexes().equals(List.of(regionIndex))) {
                continue;
            }

            RegionReading merged =
                    mapRegion(candidate.mergedReading(), templateInfo, columnsByHeader, carried);
            if (merged.mapping().score() > best.mapping().score()) {
                LOGGER.debug("Region {} read as a continued list ({}): {} rows instead of {}",
                        regionIndex, candidate.evidence(),
                        merged.mapping().records().size(), best.mapping().records().size());
                best = merged;
            }
        }

        return best;
    }

    /**
     * Three tiers, strongest first: the region's own header band, a header band carried over
     * from an earlier page, then blind positional mapping.
     */
    private RegionReading mapRegion(LayoutRegion region,
                                    ExcelTemplateInfo templateInfo,
                                    Map<String, List<ExcelColumn>> columnsByHeader,
                                    CarriedHeaderBand carried) {

        Map<Integer, List<ExcelColumn>> ownBand = headerBandOf(region, columnsByHeader);
        if (ownBand.size() >= MIN_HEADER_MATCHES) {
            return new RegionReading(
                    mapRows(region, ownBand, 1, ownBand.size()),
                    new CarriedHeaderBand(ownBand, region.columnCount(), region.page()));
        }

        if (carried != null && carried.appliesTo(region)) {
            // Every row is data here: the header stayed behind on the previous page.
            LOGGER.debug("Region on page {} continues a table whose header band was on page {}",
                    region.page(), carried.page());
            return new RegionReading(
                    mapRows(region, carried.columns(), 0, carried.columns().size()), null);
        }

        return new RegionReading(mapPositionally(region, templateInfo), null);
    }

    /** A region mapping, plus the header band it established for later pages to reuse. */
    private record RegionReading(RegionMapping mapping, CarriedHeaderBand headerBand) {
    }

    /**
     * A header band established on one page, available to the same table's continuation on
     * the next. Deliberately narrow - it applies only to a later page whose column count
     * matches - so an unrelated grid further down the document cannot inherit it.
     */
    private record CarriedHeaderBand(Map<Integer, List<ExcelColumn>> columns, int columnCount, int page) {

        boolean appliesTo(LayoutRegion region) {
            return region.page() > page && region.columnCount() == columnCount;
        }
    }

    /**
     * Matches the region's first row against the template headers and, when enough of them
     * line up, maps every following row positionally. This is the path that turns a
     * twenty-row table into twenty spreadsheet rows without a single model call.
     */
    private Map<Integer, List<ExcelColumn>> headerBandOf(
            LayoutRegion region, Map<String, List<ExcelColumn>> columnsByHeader) {

        Map<Integer, List<ExcelColumn>> templateColumnsByRegionColumn = new LinkedHashMap<>();
        if (region.rows().size() < 2) {
            return templateColumnsByRegionColumn;
        }
        for (LayoutCell cell : region.rows().get(0).cells()) {
            List<ExcelColumn> matching = columnsByHeader.get(headerFieldMapper.matchKey(cell.text()));
            if (matching != null) {
                templateColumnsByRegionColumn.put(cell.columnIndex(), matching);
            }
        }
        return templateColumnsByRegionColumn;
    }

    private RegionMapping mapRows(LayoutRegion region,
                                  Map<Integer, List<ExcelColumn>> templateColumnsByRegionColumn,
                                  int firstDataRow,
                                  int headerMatches) {

        List<MappedRecord> records = new ArrayList<>(region.rows().size());

        for (int rowIndex = firstDataRow; rowIndex < region.rows().size(); rowIndex++) {
            Map<Integer, String> values = new LinkedHashMap<>();
            Map<Integer, CellOrigin> origins = new LinkedHashMap<>();

            for (LayoutCell cell : region.rows().get(rowIndex).cells()) {
                List<ExcelColumn> targets = templateColumnsByRegionColumn.get(cell.columnIndex());
                if (targets == null || isBlank(cell.text())) {
                    continue;
                }
                for (ExcelColumn target : targets) {
                    values.put(target.columnIndex(), cell.text().strip());
                    origins.put(target.columnIndex(), new CellOrigin(region.page(), cell.bbox(),
                            "Table column matched the template header, row " + rowIndex + " of the region"));
                }
            }

            if (!values.isEmpty()) {
                records.add(new MappedRecord(values, origins));
            }
        }

        return records.isEmpty()
                ? RegionMapping.none()
                : new RegionMapping(records, headerMatches * HEADER_MATCH_WEIGHT + records.size());
    }

    /**
     * Last resort for a region whose own headings mean nothing to this template: if each row
     * yields exactly as many values as the template has columns, map them in order.
     *
     * <p>A row may have to be split to get there - {@code 1 - 3.5} is two values written into
     * one cell - and a reading whose values come out whole scores above one that leaves a
     * separator sitting inside a cell. That preference is what distinguishes a list continued
     * down two columns from two genuinely parallel readings when the template names neither.
     */
    private RegionMapping mapPositionally(LayoutRegion region, ExcelTemplateInfo templateInfo) {
        List<ExcelColumn> headers = templateInfo.headers();
        if (headers.isEmpty() || region.rows().isEmpty()) {
            return RegionMapping.none();
        }

        List<MappedRecord> records = new ArrayList<>();
        boolean allValuesAtomic = true;

        for (LayoutRow row : region.rows()) {
            List<String> values = valuesFor(row, headers.size());
            if (values.isEmpty()) {
                continue;
            }

            // Every value on this row was read from the row's own cells, so they share its
            // rectangle - a cell that had to be split has no narrower box of its own.
            CellOrigin origin = new CellOrigin(region.page(), boundsOf(row),
                    "Row position within a " + region.kind().name().toLowerCase(java.util.Locale.ROOT)
                            + " region matched the template column count");

            Map<Integer, String> rowValues = new LinkedHashMap<>();
            Map<Integer, CellOrigin> origins = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                rowValues.put(headers.get(column).columnIndex(), values.get(column));
                origins.put(headers.get(column).columnIndex(), origin);
                allValuesAtomic &= !containsSeparator(values.get(column));
            }
            records.add(new MappedRecord(rowValues, origins));
        }

        if (records.size() < MIN_POSITIONAL_RECORDS) {
            return RegionMapping.none();
        }

        return new RegionMapping(records,
                records.size() * RECORD_WEIGHT + (allValuesAtomic ? ATOMIC_VALUE_BONUS : 0));
    }

    /**
     * The row's values, in order, when it can supply exactly {@code targetCount} of them -
     * either because it already holds that many cells, or because a single composite cell
     * splits into that many. Otherwise empty, and the row is skipped.
     */
    private List<String> valuesFor(LayoutRow row, int targetCount) {
        List<String> present = new ArrayList<>(row.cells().size());
        for (LayoutCell cell : row.cells()) {
            if (!isBlank(cell.text())) {
                present.add(cell.text().strip());
            }
        }

        if (present.size() == targetCount) {
            return present;
        }

        if (present.size() == 1 && targetCount > 1) {
            return splitComposite(present.get(0), targetCount);
        }

        return List.of();
    }

    private List<String> splitComposite(String text, int targetCount) {
        for (String separator : COMPOSITE_SEPARATORS) {
            String[] parts = text.split(java.util.regex.Pattern.quote(separator));
            if (parts.length != targetCount) {
                continue;
            }

            List<String> values = new ArrayList<>(parts.length);
            for (String part : parts) {
                if (part.isBlank()) {
                    values.clear();
                    break;
                }
                values.add(part.strip());
            }
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    private boolean containsSeparator(String value) {
        for (String separator : COMPOSITE_SEPARATORS) {
            if (value.contains(separator)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Values from labelled pairs describe the whole document - an invoice number above a
     * table of line items belongs on every line. They are therefore copied into each tabular
     * record, but never over a value the row supplied itself.
     */
    private List<MappedRecord> combine(List<MappedRecord> tabularRecords,
                                       Map<Integer, String> documentLevelValues) {
        if (tabularRecords.isEmpty()) {
            return documentLevelValues.isEmpty() ? List.of() : List.of(MappedRecord.of(documentLevelValues));
        }

        List<MappedRecord> combined = new ArrayList<>(tabularRecords.size());
        for (MappedRecord record : tabularRecords) {
            combined.add(record.withDefaults(documentLevelValues));
        }
        return combined;
    }

    private com.example.ai_doc.domain.layout.BBox boundsOf(LayoutRow row) {
        com.example.ai_doc.domain.layout.BBox bounds = null;
        for (LayoutCell cell : row.cells()) {
            bounds = bounds == null ? cell.bbox() : bounds.union(cell.bbox());
        }
        return bounds == null ? new com.example.ai_doc.domain.layout.BBox(0, 0, 0, 0) : bounds;
    }

    /**
     * Maps labelled pairs, accepting both shapes the parse model produces: a "Label:" cell
     * beside its value, and "Label: value" collapsed into one cell.
     */
    private Map<Integer, String> mapKeyValueRegion(LayoutRegion region,
                                                   Map<String, List<ExcelColumn>> columnsByHeader) {
        Map<Integer, String> values = new LinkedHashMap<>();

        for (LayoutRow row : region.rows()) {
            if (row.cells().isEmpty()) {
                continue;
            }

            String label;
            String value;
            String firstCell = textOf(row.cells().get(0));

            if (row.cells().size() >= 2) {
                label = stripLabelPunctuation(firstCell);
                value = textOf(row.cells().get(1));
            } else {
                int colon = firstCell.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                label = firstCell.substring(0, colon).strip();
                value = firstCell.substring(colon + 1).strip();
            }

            if (label.isBlank() || value.isBlank()) {
                continue;
            }

            List<ExcelColumn> targets = columnsByHeader.get(headerFieldMapper.matchKey(label));
            if (targets == null) {
                continue;
            }
            for (ExcelColumn target : targets) {
                values.putIfAbsent(target.columnIndex(), value);
            }
        }

        return values;
    }

    private String stripLabelPunctuation(String label) {
        String stripped = label.strip();
        return stripped.endsWith(":") ? stripped.substring(0, stripped.length() - 1).strip() : stripped;
    }

    private String textOf(LayoutCell cell) {
        return cell.text() == null ? "" : cell.text().strip();
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
