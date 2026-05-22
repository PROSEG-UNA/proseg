package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class MaintenanceRequestException extends BaseException {

    public MaintenanceRequestException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MaintenanceRequestException notFound(String id) {
        return new MaintenanceRequestException(
                HttpStatus.NOT_FOUND,
                "MAINTENANCE_REQUEST_NOT_FOUND",
                "Solicitud de mantenimiento no encontrada con id: " + id
        );
    }

    public static MaintenanceRequestException inUse(String id) {
        return new MaintenanceRequestException(
                HttpStatus.BAD_REQUEST,
                "MAINTENANCE_REQUEST_IN_USE",
                "No se puede eliminar la solicitud de mantenimiento '" + id + "' porque tiene técnicos asociados"
        );
    }
}

