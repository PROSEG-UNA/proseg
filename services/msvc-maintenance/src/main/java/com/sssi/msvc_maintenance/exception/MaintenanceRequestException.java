package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class MaintenanceRequestException extends BaseException {

    public MaintenanceRequestException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MaintenanceRequestException notFound() {
        return new MaintenanceRequestException(
                HttpStatus.NOT_FOUND,
                "MAINTENANCE_REQUEST_NOT_FOUND",
                "No encontramos la solicitud de mantenimiento seleccionada."
        );
    }

}

