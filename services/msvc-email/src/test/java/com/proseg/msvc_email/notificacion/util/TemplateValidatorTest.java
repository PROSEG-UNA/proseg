package com.proseg.msvc_email.notificacion.util;

import com.proseg.msvc_email.notificacion.exception.EmailTemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateValidatorTest {

    private static final String PLANTILLA = "PlantillaPrueba";
    private static final String CAMPO = "nombreUsuario";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Valor null, vacío o en blanco lanza EmailTemplateException con plantilla y campo")
    void requireNotBlank_cuandoValorInvalido_lanzaExcepcionConDetalle(String valor) {
        assertThatThrownBy(() -> TemplateValidator.requireNotBlank(valor, CAMPO, PLANTILLA))
                .isInstanceOf(EmailTemplateException.class)
                .hasMessage(PLANTILLA + ": '" + CAMPO + "' es requerido");
    }

    @Test
    @DisplayName("Valor no vacío no lanza excepción")
    void requireNotBlank_cuandoValorValido_noLanzaExcepcion() {
        assertThatCode(() -> TemplateValidator.requireNotBlank("Juan", CAMPO, PLANTILLA))
                .doesNotThrowAnyException();
    }

}
