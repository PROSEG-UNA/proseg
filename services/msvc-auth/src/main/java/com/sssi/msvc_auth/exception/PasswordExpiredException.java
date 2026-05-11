package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PasswordExpiredException extends BaseException {

    public PasswordExpiredException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static PasswordExpiredException expired() {
        return new PasswordExpiredException(
                HttpStatus.FORBIDDEN,
                "PASSWORD_EXPIRED",
                "Tu contraseña expiró por seguridad. Te enviamos un correo con instrucciones para restablecerla."
        );
    }
}