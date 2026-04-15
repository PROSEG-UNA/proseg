package com.sssi.msvc_email.notificacion.service;

import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.utils.DateUtils;
import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.template.impl.GenericEmailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailEventService {

    private final EmailService emailService;

    public void sendLoginEmail(UserLoginEvent event) {

        String formattedDate = DateUtils.formatReadable(event.getTimestamp());

        GenericEmailTemplate template = GenericEmailTemplate.builder()
                .userName("usuario")
                .emailTitle("Inicio de sesión detectado")
                .emailContent(
                        "Se ha detectado un inicio de sesión en su cuenta el " +
                                formattedDate +
                                ". Si no reconoce esta actividad, por favor cambie su contraseña inmediatamente."
                )
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(event.getEmail())
                        .subject("Bienvenido a SSSI")
                        .templateDefinition(template)
                        .build()
        );

    }
}