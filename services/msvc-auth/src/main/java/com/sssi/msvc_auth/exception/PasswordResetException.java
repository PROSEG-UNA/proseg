package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PasswordResetException extends BaseException {

    public PasswordResetException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static PasswordResetException invalidToken() {
        return new PasswordResetException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_RESET_TOKEN_INVALID",
                "El token de restablecimiento es inválido o ya fue usado"
        );
    }

    public static PasswordResetException expiredToken() {
        return new PasswordResetException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_RESET_TOKEN_EXPIRED",
                "El token de restablecimiento ha expirado"
        );
    }

    public static PasswordResetException emailNotFound(String email) {
        return new PasswordResetException(
                HttpStatus.OK,
                "PASSWORD_RESET_EMAIL_PROCESSED",
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña"
        );
    }

    public static PasswordResetException tooManyRequests() {
        return new PasswordResetException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PASSWORD_RESET_RATE_LIMIT",
                "Demasiados intentos. Intente nuevamente más tarde"
        );
    }
}