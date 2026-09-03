package com.proseg.msvc_auth.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvitationException extends BaseException {

    public InvitationException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static InvitationException invalidToken() {
        return new InvitationException(
                HttpStatus.BAD_REQUEST,
                "INVITATION_TOKEN_INVALID",
                "El token de invitación es inválido o ya fue utilizado"
        );
    }

    public static InvitationException expiredToken() {
        return new InvitationException(
                HttpStatus.BAD_REQUEST,
                "INVITATION_TOKEN_EXPIRED",
                "El token de invitación ha expirado"
        );
    }

    public static InvitationException alreadyUsed() {
        return new InvitationException(
                HttpStatus.BAD_REQUEST,
                "INVITATION_TOKEN_ALREADY_USED",
                "El token de invitación ya fue utilizado"
        );
    }

    public static InvitationException userNotFound() {
        return new InvitationException(
                HttpStatus.NOT_FOUND,
                "INVITATION_USER_NOT_FOUND",
                "No se encontró el usuario asociado a la invitación"
        );
    }
}