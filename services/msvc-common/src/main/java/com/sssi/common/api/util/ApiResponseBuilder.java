package com.sssi.common.api.util;

import com.sssi.common.api.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ApiResponseBuilder {

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return build(true, data, message, null, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return build(true, data, message, null, HttpStatus.CREATED);
    }

    public static <T> ResponseEntity<ApiResponse<T>> noContent(String message) {
        return build(true, null, message, null, HttpStatus.NO_CONTENT);
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(List<String> errors, String message, HttpStatus status) {
        return build(false, null, message, errors, status);
    }

    private static <T> ResponseEntity<ApiResponse<T>> build(
            boolean success,
            T data,
            String message,
            List<String> errors,
            HttpStatus status
    ) {
        ApiResponse<T> response = new ApiResponse<>(
                success,
                message,
                data,
                errors,
                status.value()
        );

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}