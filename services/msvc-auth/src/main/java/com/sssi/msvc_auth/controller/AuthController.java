package com.sssi.msvc_auth.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.LoginRequestDto;
import com.sssi.msvc_auth.dto.LoginResponseDto;
import com.sssi.msvc_auth.dto.RegisterRequestDto;
import com.sssi.msvc_auth.dto.RegisterResponseDto;
import com.sssi.msvc_auth.dto.UserApprovalRequestDto;
import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.service.KeycloakAdminService;
import com.sssi.msvc_auth.service.KeycloakAuthService;
import com.sssi.msvc_auth.service.UserApprobationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("${routes.auth}")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;
    private final KafkaTemplate<String, UserLoginEvent> kafkaTemplate;

    @GetMapping
    public String health() {
        return "Auth Service is running";
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login attempt for user: {}", request.getIdentifier());

        String keycloakUserId = keycloakAdminService.findUserIdByIdentifier(request.getIdentifier());
        userApprobationService.assertUserIsApproved(keycloakUserId);

        String token = keycloakAuthService.getToken(
                request.getIdentifier(),
                request.getPassword()
        );

        kafkaTemplate.send(
                KafkaTopics.USER_LOGIN_TOPIC,
                UserLoginEvent.builder()
                        .email(request.getIdentifier())
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

        return ApiResponseBuilder.created(
                RegisterResponseDto.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .build(),
                "Registro exitoso. Pendiente de aprobación"
        );
    }

    @PatchMapping("/users/{id}/approval")
    public ResponseEntity<ApiResponse<String>> updateUserApproval(
            @PathVariable UUID id,
            @Valid @RequestBody UserApprovalRequestDto request
    ) {
        User updatedUser = userApprobationService.updateStatus(id, request.getStatus());

        return ApiResponseBuilder.ok(
                updatedUser.getStatus().name(),
                "Estado del usuario actualizado"
        );
    }
}