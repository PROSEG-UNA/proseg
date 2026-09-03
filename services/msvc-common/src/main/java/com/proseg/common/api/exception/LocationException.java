package com.proseg.common.api.exception;

import org.springframework.http.HttpStatus;

public class LocationException extends BaseException {

    public LocationException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static LocationException notFound(String id) {
        return new LocationException(
                HttpStatus.NOT_FOUND,
                "LOCATION_NOT_FOUND",
                "Ubicación no encontrada con id: " + id
        );
    }
}
