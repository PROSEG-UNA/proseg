package com.proseg.msvc_email.notificacion.template.impl;

import com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders.TS;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestBuilders.passwordExpiringSoon;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertCampoRequeridoFalla;
import static com.proseg.msvc_email.notificacion.template.EmailTemplateTestHelper.assertRecursosEnClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordExpiringSoonEmailTemplateTest {

    @Test
    @DisplayName("Datos válidos: plantilla, recursos y contexto")
    void buildContext_cuandoDatosValidos_cumpleContrato() {
        var template = passwordExpiringSoon();
        assertThat(template.getTemplateName()).isEqualTo("password-expiring-soon-email");
        assertRecursosEnClasspath(template);
        var ctx = template.buildContext();
        assertThat(ctx.getVariable("firstName")).isEqualTo("Ana");
        assertThat(ctx.getVariable("daysRemaining")).isEqualTo(3L);
        assertThat(ctx.getVariable("loginUrl")).isEqualTo("https://login");
        assertThat(ctx.getVariable("expiresAt")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstName", "loginUrl"})
    @DisplayName("Campo requerido null o en blanco lanza EmailTemplateException")
    void buildContext_campoRequeridoInvalido_lanzaExcepcion(String field) {
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.passwordExpiringSoonField(field, null).buildContext(), field);
        assertCampoRequeridoFalla(() -> EmailTemplateTestBuilders.passwordExpiringSoonField(field, "   ").buildContext(), field);
    }

    @Test
    @DisplayName("Reglas adicionales inválidas lanzan IllegalArgumentException")
    void buildContext_reglasAdicionalesInvalidas_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> PasswordExpiringSoonEmailTemplate.builder().firstName("Ana").daysRemaining(-1)
                .expiresAt(Instant.ofEpochMilli(TS)).loginUrl("https://login").timestamp(TS).build().buildContext())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("daysRemaining");
        assertThatThrownBy(() -> PasswordExpiringSoonEmailTemplate.builder().firstName("Ana").daysRemaining(3)
                .expiresAt(null).loginUrl("https://login").timestamp(TS).build().buildContext())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expiresAt");
        assertThatThrownBy(() -> PasswordExpiringSoonEmailTemplate.builder().firstName("Ana").daysRemaining(3)
                .expiresAt(Instant.ofEpochMilli(TS)).loginUrl("https://login").timestamp(0).build().buildContext())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timestamp");
    }

}
