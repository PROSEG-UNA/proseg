package com.proseg.msvc_auth.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PasswordChangeException extends BaseException {

    public PasswordChangeException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static PasswordChangeException confirmationMismatch() {
        return new PasswordChangeException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_CONFIRMATION_MISMATCH",
                "La nueva contraseña y su confirmación no coinciden"
        );
    }

    public static PasswordChangeException sameAsCurrent() {
        return new PasswordChangeException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_REUSE_NOT_ALLOWED",
                "La nueva contraseña no puede ser igual a la actual"
        );
    }
}
