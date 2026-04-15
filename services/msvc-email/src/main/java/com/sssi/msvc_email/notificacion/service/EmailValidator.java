package com.sssi.msvc_email.notificacion.service;

import com.sssi.msvc_email.notificacion.exception.EmailTemplateException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class EmailValidator {

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public void validate(String email) {
        if (email == null || !EMAIL_REGEX.matcher(email).matches()) {
            throw new EmailTemplateException("Dirección de email inválida: " + email);
        }
    }
}