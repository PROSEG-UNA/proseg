package com.sssi.msvc_auth.service;

import com.sssi.common.kafka.events.UserInvitedEvent;
import com.sssi.common.kafka.events.UserPasswordConfiguredEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.InvitationInfoResponseDto;
import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.entity.InvitationToken;
import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.exception.InvitationException;
import com.sssi.msvc_auth.repository.InvitationTokenRepository;
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
public class InvitationService {

    private static final long INVITATION_EXPIRY_SECONDS = 86400; // 24h

    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;
    private final InvitationTokenRepository invitationTokenRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public String generateInvitationToken(String keycloakUserId) {
        String token = UUID.randomUUID().toString();

        invitationTokenRepository.save(
                InvitationToken.builder()
                        .keycloakUserId(keycloakUserId)
                        .token(token)
                        .expiresAt(Instant.now().plusSeconds(INVITATION_EXPIRY_SECONDS))
                        .used(false)
                        .build()
        );

        return token;
    }

    @Transactional
    public void activateUserWithToken(String token, String newPassword) {
        InvitationToken invitation = invitationTokenRepository
                .findByTokenAndUsedFalse(token)
                .orElseThrow(InvitationException::invalidToken);

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw InvitationException.expiredToken();
        }

        keycloakAdminService.setPasswordAndEnable(
                invitation.getKeycloakUserId(),
                newPassword
        );
        invitation.setUsed(true);
        invitationTokenRepository.save(invitation);
        userApprobationService.activateUser(
                invitation.getKeycloakUserId()
        );

        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_PASSWORD_CONFIGURED_TOPIC,
                    UserPasswordConfiguredEvent.builder()
                            .keycloakUserId(invitation.getKeycloakUserId())
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de invitación para usuario: {}", kafkaEx.getMessage());
        }

        log.info(
                "Usuario {} activó su cuenta via invitación",
                invitation.getKeycloakUserId()
        );
    }

    @Transactional
    public void resendInvitation(String keycloakUserId) {
        User user = userApprobationService.findByKeycloakUserId(keycloakUserId);
        if (user.getStatus() != User.UserStatus.INVITED) {
            throw new IllegalStateException("El usuario ya activó su cuenta");
        }

        KeycloakUserResponseDto keycloakUser = keycloakAdminService.getUserById(keycloakUserId);
        invitationTokenRepository.invalidateAllByKeycloakUserId(keycloakUserId);
        String newToken = generateInvitationToken(keycloakUserId);

        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_INVITED_TOPIC,
                    UserInvitedEvent.builder()
                            .userId(keycloakUserId)
                            .username(keycloakUser.getUsername())
                            .email(keycloakUser.getEmail())
                            .firstName(keycloakUser.getFirstName())
                            .lastName(keycloakUser.getLastName())
                            .invitationToken(newToken)
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
            log.info("Invitación reenviada correctamente para usuario {}", keycloakUserId);
        } catch (Exception kafkaEx) {
            log.warn(
                    "No se pudo reenviar invitación para usuario {}: {}",
                    keycloakUserId,
                    kafkaEx.getMessage()
            );
        }
    }

    @Transactional(readOnly = true)
    public InvitationInfoResponseDto getInvitationInfo(String token) {
        InvitationToken invitation = invitationTokenRepository
                .findByTokenAndUsedFalse(token)
                .orElseThrow(InvitationException::invalidToken);

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw InvitationException.expiredToken();
        }

        KeycloakUserResponseDto user = keycloakAdminService.getUserById(
                invitation.getKeycloakUserId()
        );

        return InvitationInfoResponseDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }
}
