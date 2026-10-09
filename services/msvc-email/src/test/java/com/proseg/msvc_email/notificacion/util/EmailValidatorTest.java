package com.proseg.msvc_email.notificacion.util;

import com.proseg.msvc_email.notificacion.exception.EmailTemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailValidatorTest {

    private EmailValidator emailValidator;

    @BeforeEach
    void setUp() {
        emailValidator = new EmailValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b.com", "a.b+c@x-y.co"})
    @DisplayName("Emails con formato válido no lanzan excepción")
    void validate_cuandoEmailEsValido_noLanzaExcepcion(String email) {
        assertThatCode(() -> emailValidator.validate(email)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"usuario", "usuario@", "@dominio.com", "a @b.com", "a@b.com "})
    @DisplayName("Emails inválidos lanzan EmailTemplateException")
    void validate_cuandoEmailEsInvalido_lanzaEmailTemplateException(String email) {
        assertThatThrownBy(() -> emailValidator.validate(email))
                .isInstanceOf(EmailTemplateException.class)
                .hasMessageContaining("Dirección de email inválida:");
    }

}
