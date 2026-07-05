package com.sssi.msvc_email.controller;

import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.service.EmailService;
import com.sssi.msvc_email.notificacion.template.impl.GenericEmailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;
    @Value("${app.brand.name:PROSEG}")
    private String brandName;
    @Value("${app.brand.full-name:Programa de Servicios Generales}")
    private String brandFullName;


    @GetMapping("/test")
    public String send() {
        Email email = Email.builder()
                .to(List.of("isaacfelibrenes1904@gmail.com"))
                .subject("Test - " + brandName)
                .templateDefinition(
                            GenericEmailTemplate.builder()
                                .userName("NAME")
                                .emailTitle("Notificaciones - " + brandName)
                                .emailContent(
                                        "Este es un correo de prueba generado desde el microservicio de email de " + brandFullName + "."
                                )
                                .build()
                )
                .build();

        emailService.sendEmail(email);
        return "Email enviado a ";
    }
}
