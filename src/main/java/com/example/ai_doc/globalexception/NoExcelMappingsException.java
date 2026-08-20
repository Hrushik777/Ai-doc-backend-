package com.example.ai_doc.globalexception;

/** Raised when no document field can be assigned safely to a template column. */
public class NoExcelMappingsException extends RuntimeException {

    public NoExcelMappingsException(String message) {
        super(message);
    }
}
