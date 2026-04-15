package com.sssi.msvc_email.notificacion.exception;

/**
 * Thrown when a template is missing required fields or contains
 * invalid data that prevents rendering.
 */
public class EmailTemplateException extends RuntimeException {

    public EmailTemplateException(String message) {
        super(message);
    }

    public EmailTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}