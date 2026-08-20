package com.example.ai_doc.globalexception;

/** Raised when an uploaded source type cannot yet be converted to an AI image input. */
public class UnsupportedDocumentUnderstandingException extends RuntimeException {

    public UnsupportedDocumentUnderstandingException(String message) {
        super(message);
    }
}
