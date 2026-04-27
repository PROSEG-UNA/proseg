package com.sssi.common.api.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {

    private boolean success = true;
    private String message;
    private T data;
    private int status;
    private LocalDateTime timestamp;

    public ApiResponse(String message, T data, int status) {
        this.message = message;
        this.data = data;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}