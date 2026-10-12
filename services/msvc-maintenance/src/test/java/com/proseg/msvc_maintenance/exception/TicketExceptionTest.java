package com.proseg.msvc_maintenance.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TicketExceptionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryCases")
    @DisplayName("Cada factoría expone HttpStatus, código y mensaje definidos")
    void factory_cuandoSeInvoca_retornaStatusCodigoYMensaje(
            String caseName,
            Supplier<TicketException> factory,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage) {
        TicketException ex = factory.get();

        assertThat(ex.getHttpStatus()).isEqualTo(expectedStatus);
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
    }

    static Stream<Arguments> factoryCases() {
        return Stream.of(
                Arguments.of(
                        "notFound",
                        (Supplier<TicketException>) TicketException::notFound,
                        HttpStatus.NOT_FOUND,
                        "TICKET_NOT_FOUND",
                        "No se encontró el ticket seleccionado. Verifica la información e inténtalo nuevamente."
                ),
                Arguments.of(
                        "accessDenied",
                        (Supplier<TicketException>) TicketException::accessDenied,
                        HttpStatus.FORBIDDEN,
                        "TICKET_ACCESS_DENIED",
                        "No tienes permisos para consultar o modificar este ticket."
                ),
                Arguments.of(
                        "priorityForbidden",
                        (Supplier<TicketException>) TicketException::priorityForbidden,
                        HttpStatus.FORBIDDEN,
                        "TICKET_PRIORITY_FORBIDDEN",
                        "Solo administradores o usuarios con permiso pueden cambiar la prioridad del ticket."
                ),
                Arguments.of(
                        "assignableUsersForbidden",
                        (Supplier<TicketException>) TicketException::assignableUsersForbidden,
                        HttpStatus.FORBIDDEN,
                        "TICKET_ASSIGNABLE_USERS_FORBIDDEN",
                        "No tienes permisos para consultar usuarios asignables."
                ),
                Arguments.of(
                        "assignForbidden",
                        (Supplier<TicketException>) TicketException::assignForbidden,
                        HttpStatus.FORBIDDEN,
                        "TICKET_ASSIGN_FORBIDDEN",
                        "Solo administradores o usuarios con permiso pueden asignar tickets."
                ),
                Arguments.of(
                        "commentNotFound",
                        (Supplier<TicketException>) TicketException::commentNotFound,
                        HttpStatus.NOT_FOUND,
                        "TICKET_COMMENT_NOT_FOUND",
                        "No encontramos el comentario seleccionado para este ticket."
                ),
                Arguments.of(
                        "commentEditForbidden",
                        (Supplier<TicketException>) TicketException::commentEditForbidden,
                        HttpStatus.FORBIDDEN,
                        "TICKET_COMMENT_EDIT_FORBIDDEN",
                        "Solo puede editar sus comentarios."
                ),
                Arguments.of(
                        "commentDeleteForbidden",
                        (Supplier<TicketException>) TicketException::commentDeleteForbidden,
                        HttpStatus.FORBIDDEN,
                        "TICKET_COMMENT_DELETE_FORBIDDEN",
                        "Solo puede eliminar sus comentarios."
                ),
                Arguments.of(
                        "archiveUploadFailed",
                        (Supplier<TicketException>) TicketException::archiveUploadFailed,
                        HttpStatus.BAD_REQUEST,
                        "TICKET_ARCHIVE_UPLOAD_FAILED",
                        "No se pudo iniciar la carga del archivo adjunto."
                ),
                Arguments.of(
                        "missingAuthenticationToken",
                        (Supplier<TicketException>) TicketException::missingAuthenticationToken,
                        HttpStatus.UNAUTHORIZED,
                        "TICKET_AUTH_TOKEN_NOT_FOUND",
                        "No hay autenticación para solicitar el archivo."
                ),
                Arguments.of(
                        "relatedCampusUnavailable",
                        (Supplier<TicketException>) () -> TicketException.relatedCampusUnavailable("campus-1"),
                        HttpStatus.BAD_GATEWAY,
                        "TICKET_RELATED_CAMPUS_UNAVAILABLE",
                        "No se pudo obtener el recinto asociado al ticket: campus-1"
                ),
                Arguments.of(
                        "relatedBuildingUnavailable",
                        (Supplier<TicketException>) () -> TicketException.relatedBuildingUnavailable("building-1"),
                        HttpStatus.BAD_GATEWAY,
                        "TICKET_RELATED_BUILDING_UNAVAILABLE",
                        "No se pudo obtener el edificio asociado al ticket: building-1"
                ),
                Arguments.of(
                        "relatedFloorUnavailable",
                        (Supplier<TicketException>) () -> TicketException.relatedFloorUnavailable("floor-1"),
                        HttpStatus.BAD_GATEWAY,
                        "TICKET_RELATED_FLOOR_UNAVAILABLE",
                        "No se pudo obtener el piso asociado al ticket: floor-1"
                ),
                Arguments.of(
                        "relatedLocationUnavailable",
                        (Supplier<TicketException>) () -> TicketException.relatedLocationUnavailable("loc-1"),
                        HttpStatus.BAD_GATEWAY,
                        "TICKET_RELATED_LOCATION_UNAVAILABLE",
                        "No se pudo obtener la ubicación asociada al ticket: loc-1"
                ),
                Arguments.of(
                        "relatedAssetUnavailable",
                        (Supplier<TicketException>) () -> TicketException.relatedAssetUnavailable("asset-1"),
                        HttpStatus.BAD_GATEWAY,
                        "TICKET_RELATED_ASSET_UNAVAILABLE",
                        "No se pudo obtener el activo asociado al ticket: asset-1"
                ),
                Arguments.of(
                        "relatedPhotoUnavailable",
                        (Supplier<TicketException>) () -> TicketException.relatedPhotoUnavailable("photo.obj"),
                        HttpStatus.BAD_GATEWAY,
                        "TICKET_RELATED_PHOTO_UNAVAILABLE",
                        "No se pudo obtener la URL del adjunto asociado al ticket: photo.obj"
                )
        );
    }
}
