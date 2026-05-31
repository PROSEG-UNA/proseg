package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class MaintenanceTechnicianException extends BaseException {

    public MaintenanceTechnicianException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MaintenanceTechnicianException notFound() {
        return new MaintenanceTechnicianException(
                HttpStatus.NOT_FOUND,
                "MAINTENANCE_TECHNICIAN_NOT_FOUND",
                "No encontramos el tecnico de mantenimiento seleccionado."
        );
    }
}

