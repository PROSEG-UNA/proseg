package com.proseg.msvc_email.notificacion.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSendingExceptionTest {

    @Test
    @DisplayName("Constructor con mensaje expone el mensaje")
    void constructor_cuandoSoloMensaje_guardaMensaje() {
        EmailSendingException ex = new EmailSendingException("fallo al enviar");

        assertThat(ex).hasMessage("fallo al enviar");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("Constructor con mensaje y causa encadena la causa")
    void constructor_cuandoMensajeYCausa_guardaMensajeYCausa() {
        RuntimeException causa = new RuntimeException("error raíz");
        EmailSendingException ex = new EmailSendingException("fallo al enviar", causa);

        assertThat(ex).hasMessage("fallo al enviar");
        assertThat(ex.getCause()).isSameAs(causa);
    }

}
