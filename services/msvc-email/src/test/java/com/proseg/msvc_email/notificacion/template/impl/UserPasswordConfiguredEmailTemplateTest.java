package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders.userPasswordConfigured;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPasswordConfiguredEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos y contexto")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = userPasswordConfigured();
        assertThat(template.getTemplateName()).isEqualTo("user-password-configured-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("loginUrl")).isEqualTo("https://login");
        assertThat(ctx.getVariable("username")).isEqualTo("ana");
        assertThat(ctx.getVariable("timestamp")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstName", "lastName", "username", "email", "loginUrl"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userPasswordConfiguredField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.userPasswordConfiguredField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Timestamp no positivo lanza IllegalArgumentException")
    void buildContext_timestampInvalido_lanzaIllegalArgumentException() {
        var invalid = UserPasswordConfiguredEmailTemplate.builder().firstName("Ana").lastName("Lopez").username("ana")
                .email("a@b.com").loginUrl("https://login").timestamp(0).build();
        assertThatThrownBy(invalid::buildContext).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

}
