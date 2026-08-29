package com.example.ai_doc.domain.excel;

/**
 * How extracted records are placed into a template that may already hold the user's own
 * data.
 *
 * <p>The distinction matters because a template is not always empty. Writing from the row
 * below the header regardless - which is what this pipeline used to do unconditionally -
 * overwrites whatever the user had already put there.
 */
public enum ExcelWriteMode {

    /**
     * Fill the blank cells of rows that already exist, then append whatever is left below
     * the last row holding data. A populated cell is never written over.
     */
    FILL_THEN_APPEND,

    /**
     * Append every record below the last row holding data, leaving existing rows entirely
     * alone even where they have gaps.
     */
    APPEND_ONLY,

    /**
     * Write from the first data row down, replacing whatever is there. The original
     * behaviour, kept for callers that supply a genuinely empty template and want a
     * re-run to produce the same rows rather than accumulate duplicates.
     */
    OVERWRITE
}
