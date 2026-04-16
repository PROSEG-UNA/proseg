package com.sssi.msvc_email.controller;

import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.service.EmailService;
import com.sssi.msvc_email.notificacion.template.impl.GenericEmailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;


    @GetMapping("/test")
    public String send() {
        Email email = Email.builder()
                .to(List.of("isaacfelibrenes1904@gmail.com"))
                .subject("Test")
                .templateDefinition(
                            GenericEmailTemplate.builder()
                                .userName("NAME")
                                .emailTitle("SSSI - Notificaciones")
                                .emailContent(
                                        "este es un correo de prueba generado desde el microservicio sssi-email utilizando el template genérico."
                                )
                                .build()
                )
                .build();

        emailService.sendEmail(email);
        return "Email enviado a ";
    }
}
