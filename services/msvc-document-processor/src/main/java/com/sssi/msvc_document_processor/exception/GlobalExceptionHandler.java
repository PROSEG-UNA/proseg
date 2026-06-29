package com.sssi.msvc_document_processor.exception;

import com.sssi.common.api.exception.BaseException;
import com.sssi.common.api.response.ApiErrorResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@Slf4j
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ApiResponseBuilder.error(
                "El archivo supera el tamaño máximo permitido",
                List.of("MAX_UPLOAD_SIZE_EXCEEDED"),
                HttpStatus.PAYLOAD_TOO_LARGE
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return ApiResponseBuilder.error(
                "Ocurrió un error inesperado. Intenta de nuevo o contacta a soporte.",
                List.of("INTERNAL_ERROR"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
