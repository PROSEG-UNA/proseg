package com.proseg.msvc_email.notificacion.util;

import com.proseg.msvc_email.notificacion.exception.EmailTemplateException;

public class TemplateValidator {

    public static void requireNotBlank(String value, String fieldName, String templateName) {
        if (value == null || value.isBlank()) {
            throw new EmailTemplateException(
                    templateName + ": '" + fieldName + "' es requerido"
            );
        }
    }
}