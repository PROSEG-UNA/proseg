package com.proseg.common.api.exception;

import org.springframework.http.HttpStatus;

public class FloorException extends BaseException {

    public FloorException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static FloorException notFound(String id) {
        return new FloorException(
                HttpStatus.NOT_FOUND,
                "FLOOR_NOT_FOUND",
                "Piso no encontrado con id: " + id
        );
    }
}
