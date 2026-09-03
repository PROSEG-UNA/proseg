package com.proseg.common.api.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiErrorResponse {

    private boolean success = false;
    private String message;
    private List<String> errors;
    private int status;
    private String timestamp;

    public ApiErrorResponse(String message, List<String> errors, int status) {
        this.message = message;
        this.errors = errors;
        this.status = status;
        this.timestamp = LocalDateTime.now().toString();
    }
}