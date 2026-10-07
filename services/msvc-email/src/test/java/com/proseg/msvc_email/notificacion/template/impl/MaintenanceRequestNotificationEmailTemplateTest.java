package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceRequestNotificationEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos y contexto")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = MaintenanceRequestNotificationEmailTemplate.builder()
                .requestId(UUID.randomUUID()).actionLabel("Actualización")
                .companyName("Acme").status("OPEN").timestamp(EmailTemplateTestBuilders.TS)
                .changedFields(null).build();
        assertThat(template.getTemplateName()).isEqualTo("maintenance-request-notification-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("actionLabel")).isEqualTo("Actualización");
        assertThat(ctx.getVariable("companyName")).isEqualTo("Acme");
        assertThat(ctx.getVariable("changedFields")).isEqualTo(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"actionLabel", "companyName", "status"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.maintenanceNotificationField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.maintenanceNotificationField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Timestamp no positivo lanza IllegalArgumentException")
    void buildContext_timestampInvalido_lanzaIllegalArgumentException() {
        var invalid = MaintenanceRequestNotificationEmailTemplate.builder()
                .requestId(UUID.randomUUID()).actionLabel("Actualización")
                .companyName("Acme").status("OPEN").timestamp(0).build();
        assertThatThrownBy(invalid::buildContext).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

}
