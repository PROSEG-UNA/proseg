package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders.userRoleAssigned;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRoleAssignedEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos y contexto")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = userRoleAssigned();
        assertThat(template.getTemplateName()).isEqualTo("user-role-assigned-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("roleName")).isEqualTo("Operador");
        assertThat(ctx.getVariable("assignedByEmail")).isEqualTo("admin@b.com");
        assertThat(ctx.getVariable("timestamp")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstName", "lastName", "username", "email", "roleName", "assignedByFirstName",
            "assignedByLastName", "assignedByUsername", "assignedByEmail"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userRoleAssignedField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userRoleAssignedField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Timestamp no positivo lanza IllegalArgumentException")
    void buildContext_timestampInvalido_lanzaIllegalArgumentException() {
        var invalid = UserRoleAssignedEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").roleName("Operador").assignedByFirstName("Admin").assignedByLastName("Uno")
                .assignedByUsername("admin").assignedByEmail("admin@b.com").timestamp(0).build();
        assertThatThrownBy(invalid::buildContext).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

}
