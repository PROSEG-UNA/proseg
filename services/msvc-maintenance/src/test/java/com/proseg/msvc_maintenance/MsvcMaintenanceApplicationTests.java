package com.proseg.msvc_maintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MsvcMaintenanceApplicationTests {

    @Test
    @DisplayName("Instanciar la aplicación no lanza excepción")
    void constructor_cuandoSeInstancia_noLanzaExcepcion() {
        assertThatCode(MsvcMaintenanceApplication::new).doesNotThrowAnyException();
    }

}
