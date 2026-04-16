package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserException extends BaseException {

    public UserException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static UserException notFound(String email) {
        return new UserException(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND", "No existe un usuario con email: " + email
        );
    }

    public static UserException userAlreadyExists(String email) {
        return new UserException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS", "El usuario '" + email + "' ya está registrado"
        );
    }

    public static UserException passwordMismatch() {
        return new UserException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_MISMATCH", "Las contraseñas no coinciden"
        );
    }

    public static UserException weakPassword() {
        return new UserException(
                HttpStatus.BAD_REQUEST,
                "WEAK_PASSWORD", "La contraseña no cumple los requisitos de seguridad"
        );
    }

    public static UserException invalidUserIdFormat(String userId) {
        return new UserException(
                HttpStatus.BAD_REQUEST,
                "INVALID_USER_ID",
                "El id de usuario devuelto por Keycloak no es UUID válido: " + userId
        );
    }
}