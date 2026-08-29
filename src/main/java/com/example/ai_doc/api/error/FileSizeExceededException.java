package com.example.ai_doc.api.error;

public class FileSizeExceededException extends RuntimeException {

    public FileSizeExceededException(String message) {
        super(message);
    }
}