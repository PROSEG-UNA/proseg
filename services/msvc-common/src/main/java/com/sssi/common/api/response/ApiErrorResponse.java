package com.sssi.common.api.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiErrorResponse {

    private boolean success = false;
    private String message;
    private List<String> errors;
    private int status;
    private LocalDateTime timestamp;

    public ApiErrorResponse(String message, List<String> errors, int status) {
        this.message = message;
        this.errors = errors;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}