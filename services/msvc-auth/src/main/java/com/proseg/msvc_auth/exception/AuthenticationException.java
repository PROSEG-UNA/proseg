package com.proseg.msvc_auth.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {

    public AuthenticationException(String errorCode, String message) {
        super(HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException(
                "INVALID_TOKEN", "El token es inválido o ha expirado"
        );
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException(
                "INVALID_CREDENTIALS", "Credenciales incorrectas"
        );
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException(
                "TOKEN_EXPIRED", "El token ha expirado"
        );
    }

    public static AuthenticationException accountDisabled() {
        return new AuthenticationException(
                "ACCOUNT_DISABLED", "La cuenta está deshabilitada"
        );
    }

    public static AuthenticationException accountLocked() {
        return new AuthenticationException(
                "ACCOUNT_LOCKED", "La cuenta está bloqueada por intentos fallidos"
        );
    }
}