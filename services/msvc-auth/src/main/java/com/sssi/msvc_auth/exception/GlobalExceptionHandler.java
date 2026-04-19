package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import com.sssi.common.api.response.ApiErrorResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiErrorResponse> handleBase(BaseException ex) {
        return ApiResponseBuilder.error(
                ex.getMessage(),
                List.of(ex.getErrorCode()),
                ex.getHttpStatus()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ApiResponseBuilder.error("Error de validación", errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        return ApiResponseBuilder.error(
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                List.of("SPRING_ERROR"),
                HttpStatus.valueOf(ex.getStatusCode().value())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAnnotatedExceptions(Exception ex) throws Exception {
        var annotation = ex.getClass().getAnnotation(ResponseStatus.class);

        if (annotation != null) {
            return ApiResponseBuilder.error(
                    annotation.reason().isBlank() ? ex.getMessage() : annotation.reason(),
                    List.of("SPRING_ERROR"),
                    annotation.value()
            );
        }

        throw ex;
    }
}