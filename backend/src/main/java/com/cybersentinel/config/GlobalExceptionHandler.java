package com.cybersentinel.config;

import com.cybersentinel.dto.ApiError;
import com.cybersentinel.service.MLServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

/**
 * Centralised error handling so the frontend always gets a clear, structured
 * error instead of a raw stack trace or a silent failure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MLServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleMlUnavailable(MLServiceUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "ML Service Unavailable", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large", "Uploaded CSV exceeds the 10MB limit.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong while processing your request. Please try again.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message) {
        ApiError body = new ApiError(Instant.now(), status.value(), error, message);
        return ResponseEntity.status(status).body(body);
    }
}
