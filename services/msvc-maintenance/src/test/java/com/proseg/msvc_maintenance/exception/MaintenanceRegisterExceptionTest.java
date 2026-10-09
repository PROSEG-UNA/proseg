package com.proseg.msvc_maintenance.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceRegisterExceptionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryCases")
    @DisplayName("Cada factoría expone HttpStatus, código y mensaje definidos")
    void factory_cuandoSeInvoca_retornaStatusCodigoYMensaje(
            String caseName,
            Supplier<MaintenanceRegisterException> factory,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage) {
        MaintenanceRegisterException ex = factory.get();

        assertThat(ex.getHttpStatus()).isEqualTo(expectedStatus);
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
    }

    static Stream<Arguments> factoryCases() {
        return Stream.of(
                Arguments.of(
                        "notFound",
                        (Supplier<MaintenanceRegisterException>) MaintenanceRegisterException::notFound,
                        HttpStatus.NOT_FOUND,
                        "MAINTENANCE_REGISTER_NOT_FOUND",
                        "No encontramos el registro de mantenimiento seleccionado."
                ),
                Arguments.of(
                        "notPending",
                        (Supplier<MaintenanceRegisterException>) MaintenanceRegisterException::notPending,
                        HttpStatus.CONFLICT,
                        "MAINTENANCE_REGISTER_NOT_PENDING",
                        "La solicitud de mantenimiento no se encuentra en estado pendiente."
                ),
                Arguments.of(
                        "userWithoutCompany",
                        (Supplier<MaintenanceRegisterException>) MaintenanceRegisterException::userWithoutCompany,
                        HttpStatus.FORBIDDEN,
                        "MAINTENANCE_USER_WITHOUT_COMPANY",
                        "El usuario no pertenece a la empresa asignada a esta solicitud de mantenimiento."
                )
        );
    }
}
