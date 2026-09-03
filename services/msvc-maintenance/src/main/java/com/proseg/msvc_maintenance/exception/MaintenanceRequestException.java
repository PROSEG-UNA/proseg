package com.proseg.msvc_maintenance.exception;

import com.proseg.common.api.exception.BaseException;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import org.springframework.http.HttpStatus;

public class MaintenanceRequestException extends BaseException {

    public MaintenanceRequestException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MaintenanceRequestException invalidStatusTransition(MaintenanceStatus from, MaintenanceStatus to) {
        return new MaintenanceRequestException(
                HttpStatus.CONFLICT,
                "MAINTENANCE_REQUEST_INVALID_STATUS_TRANSITION",
                "No es posible cambiar el estado de la solicitud de " + from + " a " + to + "."
        );
    }

    public static MaintenanceRequestException notFound() {
        return new MaintenanceRequestException(
                HttpStatus.NOT_FOUND,
                "MAINTENANCE_REQUEST_NOT_FOUND",
                "No encontramos la solicitud de mantenimiento seleccionada."
        );
    }

    public static MaintenanceRequestException emailNotRegistered(String email) {
        return new MaintenanceRequestException(
                HttpStatus.BAD_REQUEST,
                "MAINTENANCE_REQUEST_EMAIL_NOT_REGISTERED",
                "El correo " + email + " no está registrado en la ubicación seleccionada."
        );
    }

    public static MaintenanceRequestException registeredEmailsUnavailable() {
        return new MaintenanceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MAINTENANCE_REQUEST_EMAILS_UNAVAILABLE",
                "No pudimos verificar los correos registrados de la ubicación seleccionada."
        );
    }

    public static MaintenanceRequestException companyNotAllowed() {
        return new MaintenanceRequestException(
                HttpStatus.FORBIDDEN,
                "MAINTENANCE_REQUEST_COMPANY_NOT_ALLOWED",
                "Solo puedes registrar solicitudes para tu empresa asociada."
        );
    }

}

