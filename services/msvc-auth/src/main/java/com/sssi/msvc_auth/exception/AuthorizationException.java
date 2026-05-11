package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AuthorizationException extends BaseException {

    public AuthorizationException(String errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public static AuthorizationException insufficientPermissions() {
        return new AuthorizationException(
                "INSUFFICIENT_PERMISSIONS", "No tienes permisos para realizar esta acción"
        );
    }

    public static AuthorizationException roleNotAllowed(String role) {
        return new AuthorizationException(
                "ROLE_NOT_ALLOWED", "El rol '" + role + "' no tiene acceso a este recurso"
        );
    }

    public static AuthorizationException accountNotApproved(String status) {
        return new AuthorizationException(
                "ACCOUNT_NOT_APPROVED",
                "La cuenta no esta aprobada para iniciar sesion. Estado actual: " + status
        );
    }
}