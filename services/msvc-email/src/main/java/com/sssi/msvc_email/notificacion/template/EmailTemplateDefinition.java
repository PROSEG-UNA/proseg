package com.sssi.msvc_email.notificacion.template;

import org.thymeleaf.context.Context;

import java.util.List;

/**
 * Contract that every email template must fulfill.
 * Each implementation owns:
 * - its required variables
 * - the construction of the Thymeleaf context
 * - its own validation logic
 * - its inline image declarations
 * EmailService never needs to know about template internals.
 */
public interface EmailTemplateDefinition {

    /**
     * Thymeleaf template filename (without extension).
     */
    String getTemplateName();

    /**
     * Build the fully-populated Thymeleaf context.
     * Implementations must validate required fields here and throw
     * {@link com.sssi.msvc_email.notificacion.exception.EmailTemplateException} on missing or invalid data.
     */
    Context buildContext();

    /**
     * Classpath-relative image names to attach as inline resources.
     * Return an empty list when no inline images are needed.
     */
    List<String> getInlineImages();
}