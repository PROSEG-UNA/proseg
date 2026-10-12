package com.proseg.msvc_email.notificacion.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateExceptionTest {

    @Test
    @DisplayName("Constructor con mensaje expone el mensaje")
    void constructor_cuandoSoloMensaje_guardaMensaje() {
        EmailTemplateException ex = new EmailTemplateException("plantilla inválida");

        assertThat(ex).hasMessage("plantilla inválida");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("Constructor con mensaje y causa encadena la causa")
    void constructor_cuandoMensajeYCausa_guardaMensajeYCausa() {
        IllegalArgumentException causa = new IllegalArgumentException("argumento inválido");
        EmailTemplateException ex = new EmailTemplateException("plantilla inválida", causa);

        assertThat(ex).hasMessage("plantilla inválida");
        assertThat(ex.getCause()).isSameAs(causa);
    }

}
