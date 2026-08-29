package com.example.ai_doc.api.error;

public class EmptyFileException extends RuntimeException {

    public EmptyFileException(String message) {
        super(message);
    }
}