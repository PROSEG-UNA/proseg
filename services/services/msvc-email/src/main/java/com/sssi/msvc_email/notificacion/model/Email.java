package com.sssi.msvc_email.notificacion.model;

import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import lombok.*;

/**
 * Minimal representation of an outbound email request.
 *
 * This model intentionally contains NO template-specific variables.
 * Variable ownership belongs exclusively to each EmailTemplateDefinition.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    /** Recipient email address. */
    private String to;

    /** Email subject line. */
    private String subject;

    /**
     * Fully-configured template definition.
     * Constructed by the caller with all required data already set.
     */
    private EmailTemplateDefinition templateDefinition;
}