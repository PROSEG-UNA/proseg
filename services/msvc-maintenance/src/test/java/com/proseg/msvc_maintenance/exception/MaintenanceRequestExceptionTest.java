package com.proseg.msvc_maintenance.exception;

import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceRequestExceptionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryCases")
    @DisplayName("Cada factoría expone HttpStatus, código y mensaje definidos")
    void factory_cuandoSeInvoca_retornaStatusCodigoYMensaje(
            String caseName,
            Supplier<MaintenanceRequestException> factory,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage) {
        MaintenanceRequestException ex = factory.get();

        assertThat(ex.getHttpStatus()).isEqualTo(expectedStatus);
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
    }

    static Stream<Arguments> factoryCases() {
        return Stream.of(
                Arguments.of(
                        "invalidStatusTransition",
                        (Supplier<MaintenanceRequestException>) () ->
                                MaintenanceRequestException.invalidStatusTransition(
                                        MaintenanceStatus.PENDING, MaintenanceStatus.COMPLETED),
                        HttpStatus.CONFLICT,
                        "MAINTENANCE_REQUEST_INVALID_STATUS_TRANSITION",
                        "No es posible cambiar el estado de la solicitud de PENDING a COMPLETED."
                ),
                Arguments.of(
                        "notFound",
                        (Supplier<MaintenanceRequestException>) MaintenanceRequestException::notFound,
                        HttpStatus.NOT_FOUND,
                        "MAINTENANCE_REQUEST_NOT_FOUND",
                        "No encontramos la solicitud de mantenimiento seleccionada."
                ),
                Arguments.of(
                        "emailNotRegistered",
                        (Supplier<MaintenanceRequestException>) () ->
                                MaintenanceRequestException.emailNotRegistered("a@b.com"),
                        HttpStatus.BAD_REQUEST,
                        "MAINTENANCE_REQUEST_EMAIL_NOT_REGISTERED",
                        "El correo a@b.com no está registrado en la ubicación seleccionada."
                ),
                Arguments.of(
                        "registeredEmailsUnavailable",
                        (Supplier<MaintenanceRequestException>) MaintenanceRequestException::registeredEmailsUnavailable,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "MAINTENANCE_REQUEST_EMAILS_UNAVAILABLE",
                        "No pudimos verificar los correos registrados de la ubicación seleccionada."
                ),
                Arguments.of(
                        "companyNotAllowed",
                        (Supplier<MaintenanceRequestException>) MaintenanceRequestException::companyNotAllowed,
                        HttpStatus.FORBIDDEN,
                        "MAINTENANCE_REQUEST_COMPANY_NOT_ALLOWED",
                        "Solo puedes registrar solicitudes para tu empresa asociada."
                )
        );
    }
}
