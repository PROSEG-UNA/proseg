package com.proseg.msvc_maintenance.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyExceptionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryCases")
    @DisplayName("Cada factoría expone HttpStatus, código y mensaje definidos")
    void factory_cuandoSeInvoca_retornaStatusCodigoYMensaje(
            String caseName,
            Supplier<CompanyException> factory,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage) {
        CompanyException ex = factory.get();

        assertThat(ex.getHttpStatus()).isEqualTo(expectedStatus);
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
    }

    static Stream<Arguments> factoryCases() {
        return Stream.of(
                Arguments.of(
                        "notFound",
                        (Supplier<CompanyException>) CompanyException::notFound,
                        HttpStatus.NOT_FOUND,
                        "COMPANY_NOT_FOUND",
                        "No encontramos la empresa seleccionada. Verifica la información e inténtalo nuevamente."
                ),
                Arguments.of(
                        "duplicateName",
                        (Supplier<CompanyException>) () -> CompanyException.duplicateName("Acme"),
                        HttpStatus.CONFLICT,
                        "COMPANY_DUPLICATE_NAME",
                        "Ya existe una empresa con el nombre: Acme"
                ),
                Arguments.of(
                        "duplicateLegalId",
                        (Supplier<CompanyException>) () -> CompanyException.duplicateLegalId("3-101-123456"),
                        HttpStatus.CONFLICT,
                        "COMPANY_DUPLICATE_LEGAL_ID",
                        "Ya existe una empresa con la cédula jurídica: 3-101-123456"
                ),
                Arguments.of(
                        "inUse",
                        (Supplier<CompanyException>) () -> CompanyException.inUse("Acme"),
                        HttpStatus.BAD_REQUEST,
                        "COMPANY_IN_USE",
                        "No se puede eliminar la empresa 'Acme' porque tiene registros relacionados"
                ),
                Arguments.of(
                        "noAssociatedCompany",
                        (Supplier<CompanyException>) CompanyException::noAssociatedCompany,
                        HttpStatus.NOT_FOUND,
                        "COMPANY_NO_ASSOCIATED",
                        "El usuario no tiene una empresa asociada."
                ),
                Arguments.of(
                        "invalidKeycloakUser",
                        (Supplier<CompanyException>) () -> CompanyException.invalidKeycloakUser("kc-1"),
                        HttpStatus.NOT_FOUND,
                        "INVALID_KEYCLOAK_USER",
                        "No existe un usuario en Keycloak con el id: kc-1"
                ),
                Arguments.of(
                        "userLookupFailed",
                        (Supplier<CompanyException>) CompanyException::userLookupFailed,
                        HttpStatus.BAD_GATEWAY,
                        "USER_LOOKUP_FAILED",
                        "No se pudo verificar el usuario contra el servicio de autenticacion. Intentalo de nuevo en unos minutos."
                ),
                Arguments.of(
                        "inviteUserFailed con detalle",
                        (Supplier<CompanyException>) () -> CompanyException.inviteUserFailed("Correo inválido"),
                        HttpStatus.BAD_REQUEST,
                        "COMPANY_USER_INVITATION_FAILED",
                        "Correo inválido"
                ),
                Arguments.of(
                        "inviteUserFailed sin detalle",
                        (Supplier<CompanyException>) () -> CompanyException.inviteUserFailed("  "),
                        HttpStatus.BAD_REQUEST,
                        "COMPANY_USER_INVITATION_FAILED",
                        "No se pudo invitar el usuario para esta empresa"
                )
        );
    }
}
