package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders.userStatusChanged;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserStatusChangedEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos y contexto")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = userStatusChanged();
        assertThat(template.getTemplateName()).isEqualTo("user-status-changed-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("oldStatus")).isEqualTo("Activos");
        assertThat(ctx.getVariable("newStatus")).isEqualTo("Pendientes");
        assertThat(ctx.getVariable("reason")).isEqualTo("Sin especificar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstName", "lastName", "username", "email", "oldStatus", "newStatus",
            "changedByFirstName", "changedByLastName", "changedByUsername", "changedByEmail"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userStatusChangedField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userStatusChangedField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Timestamp no positivo lanza IllegalArgumentException")
    void buildContext_timestampInvalido_lanzaIllegalArgumentException() {
        var invalid = UserStatusChangedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").oldStatus("APPROVED").newStatus("PENDING").changedByFirstName("Admin")
                .changedByLastName("Uno").changedByUsername("admin").changedByEmail("admin@b.com").timestamp(0).build();
        assertThatThrownBy(invalid::buildContext).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

}
