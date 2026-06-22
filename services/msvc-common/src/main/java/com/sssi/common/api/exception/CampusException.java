package com.sssi.common.api.exception;

import org.springframework.http.HttpStatus;

public class CampusException extends BaseException {
    public CampusException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static CampusException notFound(String id) {
        return new CampusException(
                HttpStatus.NOT_FOUND,
                "BUILDING_NOT_FOUND",
                "Campus no encontrado con id: " + id
        );
    }
}
