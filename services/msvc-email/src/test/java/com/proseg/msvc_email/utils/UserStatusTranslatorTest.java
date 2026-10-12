package com.proseg.msvc_email.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatusTranslatorTest {

    @ParameterizedTest
    @CsvSource({
            "INVITED, Invitados",
            "invited, Invitados",
            "APPROVED, Activos",
            "approved, Activos",
            "REJECTED, Inactivos",
            "rejected, Inactivos",
            "PENDING, Pendientes",
            "pending, Pendientes"
    })
    @DisplayName("Estados conocidos se traducen al español")
    void translateStatus_cuandoEstadoEsConocido_devuelveTraduccion(String entrada, String esperado) {
        assertThat(UserStatusTranslator.translateStatus(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Estado desconocido se devuelve sin cambios")
    void translateStatus_cuandoEstadoEsDesconocido_devuelveMismoValor() {
        assertThat(UserStatusTranslator.translateStatus("ARCHIVED")).isEqualTo("ARCHIVED");
    }

    @Test
    @DisplayName("Null devuelve null")
    void translateStatus_cuandoEsNull_devuelveNull() {
        assertThat(UserStatusTranslator.translateStatus(null)).isNull();
    }

}
