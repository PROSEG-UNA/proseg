package com.proseg.msvc_maintenance.entity.enums;

import com.proseg.msvc_maintenance.exception.MaintenanceRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceStatusTransitionsTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @MethodSource("canTransitionCases")
    @DisplayName("canTransition evalúa las nueve combinaciones de estado y los null")
    void canTransition_cuandoFromTo_entoncesResultadoEsperado(
            MaintenanceStatus from,
            MaintenanceStatus to,
            boolean expected) {
        assertThat(MaintenanceStatusTransitions.canTransition(from, to)).isEqualTo(expected);
    }

    static Stream<Arguments> canTransitionCases() {
        return Stream.of(
                Arguments.of(MaintenanceStatus.PENDING, MaintenanceStatus.PENDING, true),
                Arguments.of(MaintenanceStatus.PENDING, MaintenanceStatus.COMPLETED, true),
                Arguments.of(MaintenanceStatus.PENDING, MaintenanceStatus.CANCELLED, true),
                Arguments.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.PENDING, false),
                Arguments.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.COMPLETED, true),
                Arguments.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED, false),
                Arguments.of(MaintenanceStatus.CANCELLED, MaintenanceStatus.PENDING, false),
                Arguments.of(MaintenanceStatus.CANCELLED, MaintenanceStatus.COMPLETED, false),
                Arguments.of(MaintenanceStatus.CANCELLED, MaintenanceStatus.CANCELLED, true),
                Arguments.of(null, MaintenanceStatus.PENDING, false),
                Arguments.of(MaintenanceStatus.PENDING, null, false),
                Arguments.of(null, null, false)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allowedTransitions")
    @DisplayName("validateOrThrow no lanza cuando la transición está permitida")
    void validateOrThrow_cuandoTransicionPermitida_noLanza(MaintenanceStatus from, MaintenanceStatus to) {
        assertThatCode(() -> MaintenanceStatusTransitions.validateOrThrow(from, to))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(MaintenanceStatus.PENDING, MaintenanceStatus.PENDING),
                Arguments.of(MaintenanceStatus.PENDING, MaintenanceStatus.COMPLETED),
                Arguments.of(MaintenanceStatus.PENDING, MaintenanceStatus.CANCELLED),
                Arguments.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.COMPLETED),
                Arguments.of(MaintenanceStatus.CANCELLED, MaintenanceStatus.CANCELLED)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("forbiddenTransitions")
    @DisplayName("validateOrThrow lanza MaintenanceRequestException con mensaje de transición inválida")
    void validateOrThrow_cuandoTransicionNoPermitida_lanzaExcepcionConMensaje(
            MaintenanceStatus from,
            MaintenanceStatus to) {
        assertThatThrownBy(() -> MaintenanceStatusTransitions.validateOrThrow(from, to))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> {
                    MaintenanceRequestException mre = (MaintenanceRequestException) ex;
                    MaintenanceRequestException expected =
                            MaintenanceRequestException.invalidStatusTransition(from, to);
                    assertThat(mre.getHttpStatus()).isEqualTo(expected.getHttpStatus());
                    assertThat(mre.getErrorCode()).isEqualTo(expected.getErrorCode());
                    assertThat(mre.getMessage()).isEqualTo(expected.getMessage());
                });
    }

    static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.PENDING),
                Arguments.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED),
                Arguments.of(MaintenanceStatus.CANCELLED, MaintenanceStatus.PENDING),
                Arguments.of(MaintenanceStatus.CANCELLED, MaintenanceStatus.COMPLETED),
                Arguments.of(null, MaintenanceStatus.PENDING),
                Arguments.of(MaintenanceStatus.PENDING, null),
                Arguments.of(null, null)
        );
    }
}
