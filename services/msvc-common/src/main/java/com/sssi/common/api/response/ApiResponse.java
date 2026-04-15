package com.sssi.common.api.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private int status;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, T data,
                       List<String> errors, int status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

}