package com.example.ai_doc.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<String> handleEmptyFileException(
            EmptyFileException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }
    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<String> handleInvalidFileTypeException(
            InvalidFileTypeException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }
    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<String> handleFileSizeExceededException(
            FileSizeExceededException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler(InvalidExcelTemplateException.class)
    public ResponseEntity<String> handleInvalidExcelTemplateException(
            InvalidExcelTemplateException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler(AIServiceNotConfiguredException.class)
    public ResponseEntity<String> handleAIServiceNotConfiguredException(
            AIServiceNotConfiguredException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(exception.getMessage());
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<String> handleDocumentProcessingException(
            DocumentProcessingException exception) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception.getMessage());
    }

    @ExceptionHandler(ExternalAiServiceException.class)
    public ResponseEntity<String> handleExternalAiServiceException(
            ExternalAiServiceException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("The AI service could not process the request");
    }

    @ExceptionHandler({UnsupportedDocumentUnderstandingException.class, NoExcelMappingsException.class})
    public ResponseEntity<String> handleUnprocessableDocumentException(RuntimeException exception) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("File size cannot exceed 10 MB");
    }
}
