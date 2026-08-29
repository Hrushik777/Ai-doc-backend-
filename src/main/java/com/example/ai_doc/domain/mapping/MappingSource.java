package com.example.ai_doc.domain.mapping;

/** Which stage decided a value belongs in a column. */
public enum MappingSource {

    /** The field name matched a template header exactly. */
    DETERMINISTIC,

    /** The document's own layout put the value in that column - a table row, or a list. */
    STRUCTURAL,

    /** The reasoning model judged the value to belong there. */
    SEMANTIC
}
