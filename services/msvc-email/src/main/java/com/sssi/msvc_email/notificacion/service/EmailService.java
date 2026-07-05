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
    @Value("${app.brand.name:PROSEG}")
    private String brandName;
    @Value("${app.brand.full-name:Programa de Servicios Generales}")
    private String brandFullName;

    public void sendEmail(Email email) {
        emailValidator.validate(sender);

        List<String> to = email.getTo() == null ? List.of() : email.getTo();
        List<String> cc = email.getCc() == null ? List.of() : email.getCc();
        List<String> bcc = email.getBcc() == null ? List.of() : email.getBcc();

        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) {
            throw new EmailSendingException("Email debe tener al menos un destinatario");
        }

        to.forEach(emailValidator::validate);
        cc.forEach(emailValidator::validate);
        bcc.forEach(emailValidator::validate);

        EmailTemplateDefinition template = email.getTemplateDefinition();

        try {
            Context context = template.buildContext();
            context.setVariable("brandName", brandName);
            context.setVariable("brandFullName", brandFullName);

            String html = templateEngine.process(template.getTemplateName(), context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(to.isEmpty() ? new String[]{sender} : to.toArray(new String[0]));
            if (!cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }
            if (!bcc.isEmpty()) {
                helper.setBcc(bcc.toArray(new String[0]));
            }
            helper.setSubject(email.getSubject());
            helper.setText(html, true);

            attachInlineImages(helper, template.getInlineImages());

            mailSender.send(message);

            log.info("Email enviado a to={} cc={} bcc={} usando template {}",
                    to, cc, bcc, template.getTemplateName());

        } catch (Exception e) {
            throw new EmailSendingException(
                    "Error enviando email a to=" + to + " cc=" + cc + " bcc=" + bcc, e);
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