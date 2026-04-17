package com.sssi.msvc_email.notificacion.service;

import com.sssi.msvc_email.notificacion.exception.EmailSendingException;
import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.sssi.msvc_email.notificacion.util.EmailValidator;
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

    public void sendEmail(Email email) {
        emailValidator.validate(sender);

        if (email.getTo() == null || email.getTo().isEmpty()) {
            throw new EmailSendingException("Email debe tener al menos un destinatario");
        }

        email.getTo().forEach(emailValidator::validate);

        EmailTemplateDefinition template = email.getTemplateDefinition();

        try {
            Context context = template.buildContext();

            String html = templateEngine.process(template.getTemplateName(), context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(email.getTo().toArray(new String[0]));
            helper.setSubject(email.getSubject());
            helper.setText(html, true);

            attachInlineImages(helper, template.getInlineImages());

            mailSender.send(message);

            log.info("Email enviado a {} usando template {}",
                    String.join(", ", email.getTo()),
                    template.getTemplateName());

        } catch (Exception e) {
            throw new EmailSendingException(
                    "Error enviando email a " + email.getTo(), e);
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