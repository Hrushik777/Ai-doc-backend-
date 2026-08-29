package com.example.ai_doc.api.error;

import com.example.ai_doc.api.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Turns the pipeline's exceptions into HTTP responses of one consistent shape.
 *
 * <p>The division that matters here is between failures the caller caused and failures we
 * caused. The first kind names what was wrong with the request, because the caller can act
 * on it. The second kind returns a fixed sentence and puts the detail in the log, because
 * an internal message can carry file paths, SQL, or provider responses that a client has no
 * business seeing.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------- the caller can fix these

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ApiErrorResponse> handleEmptyFile(EmptyFileException exception) {
        return badRequest("EMPTY_FILE", exception.getMessage());
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFileType(InvalidFileTypeException exception) {
        return badRequest("INVALID_FILE_TYPE", exception.getMessage());
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleFileSizeExceeded(FileSizeExceededException exception) {
        return badRequest("FILE_TOO_LARGE", exception.getMessage());
    }

    @ExceptionHandler(InvalidExcelTemplateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTemplate(InvalidExcelTemplateException exception) {
        return badRequest("INVALID_TEMPLATE", exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException exception) {
        return badRequest("MISSING_REQUEST_PART",
                "Required request part '" + exception.getRequestPartName() + "' is missing");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File size cannot exceed 10 MB");
    }

    /**
     * The request was well formed and the document was readable - there was simply nothing
     * in it this template could take. That is an outcome, not a bug, so the explanation is
     * returned in full.
     */
    @ExceptionHandler({UnsupportedDocumentUnderstandingException.class, NoExcelMappingsException.class})
    public ResponseEntity<ApiErrorResponse> handleUnprocessable(RuntimeException exception) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_NOT_PROCESSABLE", exception.getMessage());
    }

    // ------------------------------------------------------------- the caller cannot

    @ExceptionHandler(AIServiceNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> handleAiNotConfigured(AIServiceNotConfiguredException exception) {
        LOGGER.error("AI service is not configured", exception);
        return respond(HttpStatus.NOT_IMPLEMENTED, "AI_NOT_CONFIGURED",
                "Document understanding is not configured on this server");
    }

    @ExceptionHandler(ExternalAiServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalAi(ExternalAiServiceException exception) {
        LOGGER.warn("External AI service call failed", exception);
        return respond(HttpStatus.BAD_GATEWAY, "AI_SERVICE_UNAVAILABLE",
                "The AI service could not process the request");
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleProcessingFailure(DocumentProcessingException exception) {
        // The message can name a file path, a template sheet, or a provider payload. Log it,
        // return a fixed sentence.
        LOGGER.error("Document processing failed", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "PROCESSING_FAILED",
                "The document could not be processed");
    }

    /**
     * Anything unmapped. Without this the container renders its own error page, whose
     * contents depend on configuration that is easy to get wrong.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    // ------------------------------------------------------------------------ helpers

    private ResponseEntity<ApiErrorResponse> badRequest(String code, String message) {
        return respond(HttpStatus.BAD_REQUEST, code, message);
    }

    private ResponseEntity<ApiErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(), code, message == null ? status.getReasonPhrase() : message));
    }
}
