package com.marketplace.userservice.exception.handler;

import com.marketplace.userservice.exception.CardLimitExceededException;
import com.marketplace.userservice.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("timestamp", LocalDateTime.now(), "status", 404, "error", "Not Found", "message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleCardLimit(CardLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("timestamp", LocalDateTime.now(), "status", 400, "error", "Bad Request", "message", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("timestamp", LocalDateTime.now(), "status", 400, "error", "Validation Failed", "messages", errors));
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("timestamp", LocalDateTime.now(), "status", 500, "error", "Internal Server Error", "message", ex.getMessage()));
    }
}