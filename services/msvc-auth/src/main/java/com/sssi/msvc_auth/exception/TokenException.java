package com.sssi.msvc_auth.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class TokenException extends BaseException {

    public TokenException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static TokenException malformed() {
        return new TokenException(
                HttpStatus.BAD_REQUEST,
                "TOKEN_MALFORMED", "El formato del token es inválido"
        );
    }

    public static TokenException refreshTokenExpired() {
        return new TokenException(
                HttpStatus.UNAUTHORIZED,
                "REFRESH_TOKEN_EXPIRED", "El refresh token ha expirado, inicia sesión nuevamente"
        );
    }

    public static TokenException refreshTokenNotFound() {
        return new TokenException(
                HttpStatus.UNAUTHORIZED,
                "REFRESH_TOKEN_NOT_FOUND", "No se encontró el refresh token"
        );
    }

    public static TokenException signatureInvalid() {
        return new TokenException(
                HttpStatus.UNAUTHORIZED,
                "TOKEN_SIGNATURE_INVALID", "La firma del token no es válida"
        );
    }

    public static TokenException notFound() {
        return new TokenException(
                HttpStatus.UNAUTHORIZED,
                "TOKEN_NOT_FOUND", "El token de autenticación no fue encontrado"
        );
    }
}