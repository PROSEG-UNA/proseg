package com.sssi.msvc_maintenance.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class TicketException extends BaseException {

    public TicketException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static TicketException notFound() {
        return new TicketException(
                HttpStatus.NOT_FOUND,
                "TICKET_NOT_FOUND",
                "No se encontró el ticket seleccionado. Verifica la información e inténtalo nuevamente."
        );
    }

    public static TicketException accessDenied() {
        return new TicketException(
                HttpStatus.FORBIDDEN,
                "TICKET_ACCESS_DENIED",
                "No tienes permisos para consultar o modificar este ticket."
        );
    }

    public static TicketException priorityForbidden() {
        return new TicketException(
                HttpStatus.FORBIDDEN,
                "TICKET_PRIORITY_FORBIDDEN",
                "Solo administradores o usuarios con permiso pueden cambiar la prioridad del ticket."
        );
    }

    public static TicketException assignableUsersForbidden() {
        return new TicketException(
                HttpStatus.FORBIDDEN,
                "TICKET_ASSIGNABLE_USERS_FORBIDDEN",
                "No tienes permisos para consultar usuarios asignables."
        );
    }

    public static TicketException assignForbidden() {
        return new TicketException(
                HttpStatus.FORBIDDEN,
                "TICKET_ASSIGN_FORBIDDEN",
                "Solo administradores o usuarios con permiso pueden asignar tickets."
        );
    }

    public static TicketException commentNotFound() {
        return new TicketException(
                HttpStatus.NOT_FOUND,
                "TICKET_COMMENT_NOT_FOUND",
                "No encontramos el comentario seleccionado para este ticket."
        );
    }

    public static TicketException commentEditForbidden() {
        return new TicketException(
                HttpStatus.FORBIDDEN,
                "TICKET_COMMENT_EDIT_FORBIDDEN",
                "Solo puede editar sus comentarios."
        );
    }

    public static TicketException commentDeleteForbidden() {
        return new TicketException(
                HttpStatus.FORBIDDEN,
                "TICKET_COMMENT_DELETE_FORBIDDEN",
                "Solo puede eliminar sus comentarios."
        );
    }

    public static TicketException archiveUploadFailed() {
        return new TicketException(
                HttpStatus.BAD_REQUEST,
                "TICKET_ARCHIVE_UPLOAD_FAILED",
                "No se pudo iniciar la carga del archivo adjunto."
        );
    }

    public static TicketException missingAuthenticationToken() {
        return new TicketException(
                HttpStatus.UNAUTHORIZED,
                "TICKET_AUTH_TOKEN_NOT_FOUND",
                "No hay autenticación para solicitar el archivo."
        );
    }
}