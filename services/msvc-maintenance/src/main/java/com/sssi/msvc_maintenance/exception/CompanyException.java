package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CompanyException extends BaseException {

    public CompanyException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static CompanyException notFound() {
        return new CompanyException(
                HttpStatus.NOT_FOUND,
                "COMPANY_NOT_FOUND",
                "No encontramos la empresa seleccionada. Verifica la información e inténtalo nuevamente."
        );
    }

    public static CompanyException duplicateName(String name) {
        return new CompanyException(
                HttpStatus.CONFLICT,
                "COMPANY_DUPLICATE_NAME",
                "Ya existe una empresa con el nombre: " + name
        );
    }

    public static CompanyException duplicateLegalId(String legalId) {
        return new CompanyException(
                HttpStatus.CONFLICT,
                "COMPANY_DUPLICATE_LEGAL_ID",
                "Ya existe una empresa con la cédula jurídica: " + legalId
        );
    }

    public static CompanyException inUse(String name) {
        return new CompanyException(
                HttpStatus.BAD_REQUEST,
                "COMPANY_IN_USE",
                "No se puede eliminar la empresa '" + name + "' porque tiene registros relacionados"
        );
    }

    public static CompanyException invalidKeycloakUser(String userId) {
        return new CompanyException(
                HttpStatus.NOT_FOUND,
                "INVALID_KEYCLOAK_USER",
                "No existe un usuario en Keycloak con el id: " + userId
        );
    }

    public static CompanyException inviteUserFailed(String detail) {
        return new CompanyException(
                HttpStatus.BAD_REQUEST,
                "COMPANY_USER_INVITATION_FAILED",
                detail != null && !detail.isBlank()
                        ? detail
                        : "No se pudo invitar el usuario para esta empresa"
        );
    }
}

