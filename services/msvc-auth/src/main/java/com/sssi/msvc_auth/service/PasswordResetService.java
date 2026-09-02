package com.sssi.msvc_auth.service;

import com.sssi.common.kafka.events.PasswordChangedEvent;
import com.sssi.common.kafka.events.PasswordExpiredResetRequiredEvent;
import com.sssi.common.kafka.events.PasswordResetRequestedEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.entity.PasswordResetToken;
import com.sssi.msvc_auth.exception.PasswordChangeException;
import com.sssi.msvc_auth.exception.PasswordResetException;
import com.sssi.msvc_auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final long EXPIRY_SECONDS = 3600; // 1 hora

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakAuthService keycloakAuthService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void requestPasswordReset(String email) {
        try {
            String keycloakUserId = keycloakAdminService.findUserIdByEmail(email);
            passwordResetTokenRepository.invalidateAllByKeycloakUserId(keycloakUserId);
            String token = UUID.randomUUID().toString();
            passwordResetTokenRepository.save(
                    PasswordResetToken.builder()
                            .keycloakUserId(keycloakUserId)
                            .token(token)
                            .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                            .used(false)
                            .build()
            );
            var keycloakUser = keycloakAdminService.getUserById(keycloakUserId);
            kafkaTemplate.send(
                    KafkaTopics.PASSWORD_RESET_REQUESTED_TOPIC,
                    PasswordResetRequestedEvent.builder()
                            .keycloakUserId(keycloakUserId)
                            .email(email)
                            .firstName(keycloakUser.getFirstName())
                            .resetToken(token)
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
            log.info("Token de restablecimiento generado para keycloakUserId={}", keycloakUserId);
        } catch (PasswordResetException e) {
            log.warn("Solicitud de reset para email no registrado: {}", email);
        } catch (Exception e) {
            log.error("Error procesando solicitud de reset para {}: {}", email, e.getMessage(), e);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalse(token)
                .orElseThrow(PasswordResetException::invalidToken);
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw PasswordResetException.expiredToken();
        }
        keycloakAdminService.resetPassword(resetToken.getKeycloakUserId(), newPassword);
        passwordPolicyService.recordPasswordChange(
                resetToken.getKeycloakUserId()
        );
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Contraseña restablecida para keycloakUserId={}", resetToken.getKeycloakUserId());
        try {
            kafkaTemplate.send(
                    KafkaTopics.PASSWORD_CHANGED_TOPIC,
                    PasswordChangedEvent.builder()
                            .keycloakUserId(resetToken.getKeycloakUserId())
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
            log.info("PasswordChangedEvent enviado para userId={}",
                    resetToken.getKeycloakUserId());
        } catch (Exception e) {
            log.error("Error enviando PasswordChangedEvent userId={}: {}",
                    resetToken.getKeycloakUserId(), e.getMessage(), e);
        }
    }

    @Transactional
    public void changePassword(String keycloakUserId, String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw PasswordChangeException.confirmationMismatch();
        }

        if (currentPassword.equals(newPassword)) {
            throw PasswordChangeException.sameAsCurrent();
        }

        KeycloakUserResponseDto keycloakUser = keycloakAdminService.getUserById(keycloakUserId);
        keycloakAuthService.getToken(keycloakUser.getUsername(), currentPassword);

        keycloakAdminService.resetPassword(keycloakUserId, newPassword);
        passwordPolicyService.recordPasswordChange(keycloakUserId);

        try {
            kafkaTemplate.send(
                    KafkaTopics.PASSWORD_CHANGED_TOPIC,
                    PasswordChangedEvent.builder()
                            .keycloakUserId(keycloakUserId)
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
            log.info("PasswordChangedEvent enviado para userId={}", keycloakUserId);
        } catch (Exception e) {
            log.error("Error enviando PasswordChangedEvent userId={}: {}",
                    keycloakUserId, e.getMessage(), e);
        }
    }

    @Transactional
    public void requestExpiredPasswordReset(String keycloakUserId) {
        passwordResetTokenRepository.invalidateAllByKeycloakUserId(keycloakUserId);
        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(
                PasswordResetToken.builder()
                        .keycloakUserId(keycloakUserId)
                        .token(token)
                        .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                        .used(false)
                        .build()
        );
        var keycloakUser = keycloakAdminService.getUserById(keycloakUserId);
        kafkaTemplate.send(
                KafkaTopics.PASSWORD_EXPIRED_RESET_REQUIRED_TOPIC,
                PasswordExpiredResetRequiredEvent.builder()
                        .keycloakUserId(keycloakUserId)
                        .email(keycloakUser.getEmail())
                        .firstName(keycloakUser.getFirstName())
                        .resetToken(token)
                        .timestamp(Instant.now().toEpochMilli())
                        .build()
        );
        log.info(
                "Reset obligatorio por expiración generado para userId={}",
                keycloakUserId
        );
    }
}