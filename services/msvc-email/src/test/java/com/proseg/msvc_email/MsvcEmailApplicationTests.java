package com.proseg.msvc_email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MsvcEmailApplicationTests {

    @Test
    @DisplayName("Instanciar la clase principal no requiere contexto Spring")
    void instanciarClasePrincipal_cuandoSeCrea_noLanzaExcepcion() {
        assertThatCode(MsvcEmailApplication::new).doesNotThrowAnyException();
    }

}
