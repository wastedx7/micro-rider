package com.ridehailing.user.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException exception,
                                                HttpServletRequest request) {
        String code = exception.getReason() == null ? "INTERNAL_ERROR" : exception.getReason();
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return response(status, code, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                    HttpServletRequest request) {
        String field = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField()).orElse(null);
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_INVALID_INPUT", request, field);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception,
                                                    HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code,
                                                   HttpServletRequest request) {
        return response(status, code, request, null);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code,
                                                   HttpServletRequest request, String field) {
        String correlationId = request.getHeader("X-Request-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return ResponseEntity.status(status).body(new ErrorResponse(
                code, status.getReasonPhrase(), status.value() >= 500,
                correlationId, field, Map.of("path", request.getRequestURI())));
    }

    public record ErrorResponse(String code, String message, boolean retryable,
                                String correlationId, String field, Map<String, String> metadata) {
    }
}
