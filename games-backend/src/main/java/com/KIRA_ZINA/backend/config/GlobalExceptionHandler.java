package com.KIRA_ZINA.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String message, LocalDateTime timestamp, String path) {
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
    public ResponseEntity<ErrorResponse> handleSecurityException(java.lang.SecurityException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse error = new ErrorResponse(ex.getMessage(), LocalDateTime.now(), path);
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse error = new ErrorResponse("An unexpected error occurred: " + ex.getMessage(), LocalDateTime.now(), path);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
