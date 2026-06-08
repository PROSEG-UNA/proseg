package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class MaintenanceRegisterException extends BaseException {

    public MaintenanceRegisterException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MaintenanceRegisterException notFound() {
        return new MaintenanceRegisterException(
                HttpStatus.NOT_FOUND,
                "MAINTENANCE_REGISTER_NOT_FOUND",
                "No encontramos el registro de mantenimiento seleccionado."
        );
    }

    public static MaintenanceRegisterException notPending() {
        return new MaintenanceRegisterException(
                HttpStatus.CONFLICT,
                "MAINTENANCE_REGISTER_NOT_PENDING",
                "La solicitud de mantenimiento no se encuentra en estado pendiente."
        );
    }

    public static MaintenanceRegisterException userWithoutCompany() {
        return new MaintenanceRegisterException(
                HttpStatus.FORBIDDEN,
                "MAINTENANCE_USER_WITHOUT_COMPANY",
                "El usuario no pertenece a la empresa asignada a esta solicitud de mantenimiento."
        );
    }
}
