package com.sssi.msvc_auth.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.kafka.events.UserRegisteredEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.*;
import com.sssi.msvc_auth.service.KeycloakAdminService;
import com.sssi.msvc_auth.service.KeycloakAuthService;
import com.sssi.msvc_auth.service.UserApprobationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

@RestController
@RequestMapping("${routes.auth}")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @GetMapping
    public String health() {
        return "Auth Service is running";
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response
    ) {
        log.info("Login attempt for identifier: {}", request.getIdentifier());

        KeycloakTokenDto tokens = keycloakAuthService.getToken(
                request.getIdentifier(),
                request.getPassword()
        );

        String keycloakUserId = keycloakAuthService.extractUserIdFromToken(tokens.getAccessToken());
        userApprobationService.assertUserIsApproved(keycloakUserId);

        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_LOGIN_TOPIC,
                    UserLoginEvent.builder()
                            .userId(keycloakUserId)
                            .identifier(request.getIdentifier())
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de login a Kafka: {}", kafkaEx.getMessage());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("auth_token", tokens.getAccessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", tokens.getRefreshToken()).toString());

        log.info("Login exitoso para identifier: {}", request.getIdentifier());

        return ApiResponseBuilder.ok(
                LoginResponseDto.builder()
                        .identifier(request.getIdentifier())
                        .build(),
                "Login exitoso"
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expireCookie("auth_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expireCookie("refresh_token").toString());
        return ApiResponseBuilder.ok(null, "Logout exitoso");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<KeycloakTokenDto>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshTokenValue = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenValue = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw com.sssi.msvc_auth.exception.TokenException.refreshTokenNotFound();
        }

        KeycloakTokenDto tokens = keycloakAuthService.refreshToken(refreshTokenValue);

        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("auth_token", tokens.getAccessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", tokens.getRefreshToken()).toString());

        log.info("Token renovado exitosamente");
        return ApiResponseBuilder.ok(tokens, "Token renovado exitosamente");
    }

    private ResponseCookie buildCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie expireCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request,
            @RequestHeader(value = "Accept", required = false) String acceptHeader
    ) {
        String keycloakUserId = keycloakAdminService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        userApprobationService.createPendingUser(keycloakUserId);

        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_REGISTERED_TOPIC,
                    UserRegisteredEvent.builder()
                            .userId(keycloakUserId)
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de registro a Kafka: {}", kafkaEx.getMessage());
        }

        return ApiResponseBuilder.created(
                RegisterResponseDto.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .build(),
                "Registro exitoso. Pendiente de aprobacion"
        );
    }

}