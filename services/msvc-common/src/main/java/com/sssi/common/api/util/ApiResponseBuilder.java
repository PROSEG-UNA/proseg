package com.sssi.common.api.util;

import com.sssi.common.api.response.ApiErrorResponse;
import com.sssi.common.api.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ApiResponseBuilder {

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return buildSuccess(data, message, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return buildSuccess(data, message, HttpStatus.CREATED);
    }

    public static ResponseEntity<ApiResponse<Void>> noContent(String message) {
        return buildSuccess(null, message, HttpStatus.OK);
    }

    public static ResponseEntity<ApiErrorResponse> error(String message, List<String> errors, HttpStatus status) {
        return buildError(message, errors, status);
    }

    public static ResponseEntity<ApiErrorResponse> error(String message, HttpStatus status) {
        return buildError(message, List.of(), status);
    }

    private static <T> ResponseEntity<ApiResponse<T>> buildSuccess(T data, String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiResponse<>(message, data, status.value()));
    }

    private static ResponseEntity<ApiErrorResponse> buildError(String message, List<String> errors, HttpStatus status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiErrorResponse(message, errors, status.value()));
    }
}