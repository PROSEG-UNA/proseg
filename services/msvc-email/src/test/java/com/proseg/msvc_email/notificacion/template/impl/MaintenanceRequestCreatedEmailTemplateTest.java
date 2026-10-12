package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceRequestCreatedEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos, contexto e imágenes")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = MaintenanceRequestCreatedEmailTemplate.builder()
                .companyName("Acme").status("OPEN").timestamp(EmailTemplateTestBuilders.TS)
                .technicianNames(null).build();
        assertThat(template.getTemplateName()).isEqualTo("maintenance-request-created-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("companyName")).isEqualTo("Acme");
        assertThat(ctx.getVariable("status")).isEqualTo("OPEN");
        assertThat(ctx.getVariable("technicianNames")).isEqualTo(List.of());
        assertThat(ctx.getVariable("timestamp")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"companyName", "status"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.maintenanceCreatedField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.maintenanceCreatedField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Timestamp no positivo lanza IllegalArgumentException")
    void buildContext_timestampInvalido_lanzaIllegalArgumentException() {
        var invalid = MaintenanceRequestCreatedEmailTemplate.builder()
                .companyName("Acme").status("OPEN").timestamp(0).build();
        assertThatThrownBy(invalid::buildContext).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

}
