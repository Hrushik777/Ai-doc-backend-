package com.example.ai_doc.api.error;

/** Raised when no document field can be assigned safely to a template column. */
public class NoExcelMappingsException extends RuntimeException {

    public NoExcelMappingsException(String message) {
        super(message);
    }
}
