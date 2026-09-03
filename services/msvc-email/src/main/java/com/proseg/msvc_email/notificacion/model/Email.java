package com.proseg.msvc_email.notificacion.model;

import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import lombok.*;

import java.util.List;

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

    /** Recipients email addresses */
    private List<String> to;

    private List<String> cc;

    /** Blind carbon copy recipients. */
    private List<String> bcc;

    /** Email subject line. */
    private String subject;

    /**
     * Fully-configured template definition.
     * Constructed by the caller with all required data already set.
     */
    private EmailTemplateDefinition templateDefinition;
}