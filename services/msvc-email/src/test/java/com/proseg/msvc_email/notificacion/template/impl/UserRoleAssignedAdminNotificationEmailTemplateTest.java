package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders.userRoleAssignedAdmin;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRoleAssignedAdminNotificationEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos y contexto")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = userRoleAssignedAdmin();
        assertThat(template.getTemplateName()).isEqualTo("user-role-assigned-admin-notification-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("userEmail")).isEqualTo("a@b.com");
        assertThat(ctx.getVariable("roleName")).isEqualTo("Operador");
        assertThat(ctx.getVariable("timestamp")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"userFirstName", "userLastName", "username", "userEmail", "roleName", "assignedByFirstName",
            "assignedByLastName", "assignedByUsername", "assignedByEmail"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userRoleAssignedAdminField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userRoleAssignedAdminField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Timestamp no positivo lanza IllegalArgumentException")
    void buildContext_timestampInvalido_lanzaIllegalArgumentException() {
        var invalid = UserRoleAssignedAdminNotificationEmailTemplate.builder().userFirstName("Ana").userLastName("Lopez")
                .username("ana").userEmail("a@b.com").roleName("Operador").assignedByFirstName("Admin")
                .assignedByLastName("Uno").assignedByUsername("admin").assignedByEmail("admin@b.com").timestamp(0).build();
        assertThatThrownBy(invalid::buildContext).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

}
