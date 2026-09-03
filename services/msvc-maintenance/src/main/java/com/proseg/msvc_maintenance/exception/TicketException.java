package com.proseg.msvc_maintenance.exception;

import com.proseg.common.api.exception.BaseException;
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

    public static TicketException relatedCampusUnavailable(String campusId) {
        return new TicketException(
                HttpStatus.BAD_GATEWAY,
                "TICKET_RELATED_CAMPUS_UNAVAILABLE",
                "No se pudo obtener el recinto asociado al ticket: " + campusId
        );
    }

    public static TicketException relatedBuildingUnavailable(String buildingId) {
        return new TicketException(
                HttpStatus.BAD_GATEWAY,
                "TICKET_RELATED_BUILDING_UNAVAILABLE",
                "No se pudo obtener el edificio asociado al ticket: " + buildingId
        );
    }

    public static TicketException relatedFloorUnavailable(String floorId) {
        return new TicketException(
                HttpStatus.BAD_GATEWAY,
                "TICKET_RELATED_FLOOR_UNAVAILABLE",
                "No se pudo obtener el piso asociado al ticket: " + floorId
        );
    }

    public static TicketException relatedLocationUnavailable(String locationId) {
        return new TicketException(
                HttpStatus.BAD_GATEWAY,
                "TICKET_RELATED_LOCATION_UNAVAILABLE",
                "No se pudo obtener la ubicación asociada al ticket: " + locationId
        );
    }

    public static TicketException relatedAssetUnavailable(String assetId) {
        return new TicketException(
                HttpStatus.BAD_GATEWAY,
                "TICKET_RELATED_ASSET_UNAVAILABLE",
                "No se pudo obtener el activo asociado al ticket: " + assetId
        );
    }

    public static TicketException relatedPhotoUnavailable(String objectName) {
        return new TicketException(
                HttpStatus.BAD_GATEWAY,
                "TICKET_RELATED_PHOTO_UNAVAILABLE",
                "No se pudo obtener la URL del adjunto asociado al ticket: " + objectName
        );
    }
}