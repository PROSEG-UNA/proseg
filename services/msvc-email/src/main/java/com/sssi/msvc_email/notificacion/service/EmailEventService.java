package com.sssi.msvc_email.notificacion.service;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.utils.DateUtils;
import com.sssi.msvc_email.notificacion.client.AuthClient;
import com.sssi.msvc_email.notificacion.dto.KeycloakUserDto;
import com.sssi.msvc_email.notificacion.model.Email;
import com.sssi.msvc_email.notificacion.template.impl.GenericEmailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailEventService {

    private final EmailService emailService;
    private final AuthClient authClient;

    public void sendLoginEmail(UserLoginEvent event) {

        ApiResponse<KeycloakUserDto> apiResponse = authClient.getUserById(event.getUserId());
        KeycloakUserDto user = apiResponse.getData();

        String formattedDate = DateUtils.formatReadable(event.getTimestamp());

        GenericEmailTemplate template = GenericEmailTemplate.builder()
                .userName(user.getFirstName())
                .emailTitle("Inicio de sesión detectado")
                .emailContent(
                        "Se ha detectado un inicio de sesión en su cuenta el " +
                                formattedDate +
                                ". Si no reconoce esta actividad, por favor cambie su contraseña inmediatamente."
                )
                .build();

        emailService.sendEmail(
                Email.builder()
                        .to(List.of(user.getEmail()))
                        .subject("Alerta de seguridad - SSSI")
                        .templateDefinition(template)
                        .build()
        );
    }
}