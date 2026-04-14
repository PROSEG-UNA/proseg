package com.sssi.msvc_email.notificacion.service;

import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailValidator emailValidator;

    @Value("${spring.mail.username}")
    private String sender;

    /**
     * Sends an email using the template definition embedded in the request.
     * <p>
     * The service is responsible for:
     * 1. Validating addresses
     * 2. Delegating context construction to the template
     * 3. Rendering HTML via Thymeleaf
     * 4. Attaching inline images declared by the template
     * 5. Dispatching the message
     * <p>
     * It does NOT know anything about individual template variables.
     */
    public void sendEmail(Email email) {
        emailValidator.validate(sender);
        emailValidator.validate(email.getTo());

        EmailTemplateDefinition template = email.getTemplateDefinition();

        try {
            Context context = template.buildContext();

            String html = templateEngine.process(template.getTemplateName(), context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setText(html, true);

            attachInlineImages(helper, template.getInlineImages());

            mailSender.send(message);
            log.info("Email enviado a {} usando template {}", email.getTo(), email.getTemplateDefinition().getTemplateName());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error enviando email a " + email.getTo() + ": " + e.getMessage(), e);
        }
    }

    private void attachInlineImages(MimeMessageHelper helper, List<String> images) {
        if (images == null || images.isEmpty()) return;

        images.forEach(imageName -> {
            try {
                helper.addInline(imageName, new ClassPathResource("images/" + imageName));
            } catch (Exception e) {
                log.warn("No se pudo adjuntar imagen inline '{}': {}", imageName, e.getMessage());
            }
        });
    }
}