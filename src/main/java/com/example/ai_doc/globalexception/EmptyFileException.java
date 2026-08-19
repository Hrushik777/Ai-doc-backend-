package com.example.ai_doc.globalexception;

public class EmptyFileException extends RuntimeException {

    public EmptyFileException(String message) {
        super(message);
    }
}