package com.proseg.msvc_forms.exception;

import com.proseg.common.api.response.ApiErrorResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_forms.validator.FormValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FormValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleFormValidationException(FormValidationException ex) {
        log.warn("Form validation error: {}", ex.getMessage());
        return ApiResponseBuilder.error(
                ex.getMessage(),
                List.of(ex.getErrorCode()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return ApiResponseBuilder.error("Error de validación", errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ApiResponseBuilder.error(
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                List.of("SPRING_ERROR"),
                status
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiResponseBuilder.error(
                "Error interno del servidor",
                List.of("INTERNAL_ERROR"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Método HTTP no permitido: {}", ex.getMethod());

        ResponseEntity<ApiErrorResponse> response = ApiResponseBuilder.error(
                "Método HTTP no permitido",
                List.of("METHOD_NOT_ALLOWED"),
                HttpStatus.METHOD_NOT_ALLOWED
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .headers(ex.getHeaders())
                .body(response.getBody());
    }
}