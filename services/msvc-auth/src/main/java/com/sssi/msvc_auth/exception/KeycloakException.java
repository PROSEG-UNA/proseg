package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class KeycloakException extends BaseException {

    public KeycloakException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static KeycloakException roleNotFound(String roleName) {
        return new KeycloakException(
                HttpStatus.NOT_FOUND,
                "ROLE_NOT_FOUND",
                "El rol '" + roleName + "' no existe en Keycloak"
        );
    }

    public static KeycloakException userNotFound(String userId) {
        return new KeycloakException(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "El usuario con id '" + userId + "' no existe en Keycloak"
        );
    }

    public static KeycloakException assignmentError(String message) {
        return new KeycloakException(
                HttpStatus.BAD_REQUEST,
                "ROLE_ASSIGNMENT_ERROR",
                message
        );
    }

    public static KeycloakException requiresAssociatedCompany() {
        return new KeycloakException(
                HttpStatus.BAD_REQUEST,
                "ROLE_REQUIRES_ASSOCIATED_COMPANY",
                "No se puede asignar el rol de solicitante: el usuario no tiene una empresa asociada."
        );
    }

    public static KeycloakException generic(String message) {
        return new KeycloakException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "KEYCLOAK_ERROR",
                message
        );
    }
}