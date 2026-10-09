package com.proseg.msvc_maintenance.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UserCompanyExceptionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("factoryCases")
    @DisplayName("Cada factoría expone HttpStatus, código y mensaje definidos")
    void factory_cuandoSeInvoca_retornaStatusCodigoYMensaje(
            String caseName,
            Supplier<UserCompanyException> factory,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage) {
        UserCompanyException ex = factory.get();

        assertThat(ex.getHttpStatus()).isEqualTo(expectedStatus);
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
    }

    static Stream<Arguments> factoryCases() {
        return Stream.of(
                Arguments.of(
                        "notFound",
                        (Supplier<UserCompanyException>) UserCompanyException::notFound,
                        HttpStatus.NOT_FOUND,
                        "USER_COMPANY_NOT_FOUND",
                        "No encontramos la asignacion de empresa solicitada."
                ),
                Arguments.of(
                        "duplicateRelation",
                        (Supplier<UserCompanyException>) UserCompanyException::duplicateRelation,
                        HttpStatus.CONFLICT,
                        "USER_COMPANY_DUPLICATE",
                        "Este usuario ya tiene asignada esa empresa."
                )
        );
    }
}
