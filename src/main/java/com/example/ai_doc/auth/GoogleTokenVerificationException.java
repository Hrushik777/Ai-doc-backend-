package com.example.ai_doc.auth;

/** A Google ID token that could not be trusted: bad signature, wrong audience, expired, or barred. */
public class GoogleTokenVerificationException extends RuntimeException {

    public GoogleTokenVerificationException(String message) {
        super(message);
    }

    public GoogleTokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
