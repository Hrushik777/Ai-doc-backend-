package com.example.ai_doc.globalexception;

public class InvalidExcelTemplateException extends RuntimeException {

    public InvalidExcelTemplateException(String message) {
        super(message);
    }

    public InvalidExcelTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
