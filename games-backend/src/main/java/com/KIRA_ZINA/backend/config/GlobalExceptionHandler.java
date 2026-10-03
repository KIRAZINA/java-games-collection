package com.KIRA_ZINA.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String message, LocalDateTime timestamp, String path) {
    }

    @ExceptionHandler(com.KIRA_ZINA.backend.common.exception.ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(com.KIRA_ZINA.backend.common.exception.ResourceNotFoundException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("status", 404);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Unmapped/unexposed paths (e.g. unexposed actuator endpoints) surface as
    // NoResourceFoundException; map them to 404 instead of the generic 500.
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("status", 404);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Wrong HTTP method on a mapped path (e.g. PUT on a POST-only endpoint) must be
    // 405 Method Not Allowed, not the generic 500 from the catch-all handler. Same
    // {"error","status"} shape as the 404/403 handlers; supported methods folded into
    // the error message.
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String error = ex.getMessage();
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            error = error + ". Supported: " + String.join(", ",
                    ex.getSupportedHttpMethods().stream().map(m -> m.name()).toList());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("status", 405);
        return new ResponseEntity<>(body, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse error = new ErrorResponse(ex.getMessage(), LocalDateTime.now(), path);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse error = new ErrorResponse("Invalid request format: " + ex.getMessage(), LocalDateTime.now(), path);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse error = new ErrorResponse(ex.getMessage(), LocalDateTime.now(), path);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(java.lang.SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(java.lang.SecurityException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("status", 403);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse error = new ErrorResponse("An unexpected error occurred: " + ex.getMessage(), LocalDateTime.now(), path);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
