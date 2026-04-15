package com.sssi.msvc_auth.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.*;
import com.sssi.msvc_auth.service.KeycloakAdminService;
import com.sssi.msvc_auth.service.KeycloakAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("${routes.auth}")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final KeycloakAdminService keycloakAdminService;
    private final KafkaTemplate<String, UserLoginEvent> kafkaTemplate;

    @GetMapping
    public String health() {
        return "Auth Service is running";
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login attempt for user: {}", request.getIdentifier());

        String token = keycloakAuthService.getToken(
                request.getIdentifier(),
                request.getPassword()
        );

        kafkaTemplate.send(
                KafkaTopics.USER_LOGIN_TOPIC,
                UserLoginEvent.builder()
                        .email(request.getIdentifier()) // FIXME: Identifier can be username or email
                        .timestamp(Instant.now().toEpochMilli())
                        .build()
        );

        log.info("Login exitoso para usuario: {}", request.getIdentifier());

        return ApiResponseBuilder.ok(
                LoginResponseDto.builder()
                        .token(token)
                        .identifier(request.getIdentifier())
                        .build(),
                "Login exitoso"
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Register attempt for user: {}", request.getUsername());

        keycloakAdminService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        String token = keycloakAuthService.getToken(
                request.getUsername(),
                request.getPassword()
        );

        log.info("Registro exitoso para usuario: {}", request.getUsername());

        return ApiResponseBuilder.created(
                RegisterResponseDto.builder()
                        .token(token)
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .build(),
                "Registro exitoso"
        );
    }
}