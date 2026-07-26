package com.sssi.msvc_auth.service;

import com.sssi.common.api.response.PagedResponse;
import com.sssi.common.kafka.events.*;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.client.MaintenanceCompanyClient;
import com.sssi.msvc_auth.dto.*;
import com.sssi.msvc_auth.entity.InvitationToken;
import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.exception.InvitationException;
import com.sssi.msvc_auth.exception.KeycloakException;
import com.sssi.msvc_auth.exception.UserException;
import com.sssi.msvc_auth.repository.InvitationTokenRepository;
import com.sssi.msvc_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private static final String ARCHIVE_FILE_ROUTE_PREFIX = "/api/v1/archive/files/";

    private static final String SOLICITAR_MANTENIMIENTO = "SOLICITAR_MANTENIMIENTO";
    private static final String SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO = "SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO";

    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;
    private final PasswordPolicyService passwordPolicyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final InvitationTokenRepository invitationTokenRepository;
    private final MaintenanceCompanyClient maintenanceCompanyClient;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<KeycloakUserResponseDto> getAllUsers(Pageable pageable) {
        PagedResponse<KeycloakUserResponseDto> response = keycloakAdminService.getAllUsers(pageable);
        List<KeycloakUserResponseDto> content = response.content();
        if (content == null || content.isEmpty()) {
            return response;
        }

        List<String> keycloakUserIds = content.stream()
                .map(KeycloakUserResponseDto::getId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        Map<String, User> userByKeycloakId = userRepository.findAllByKeycloakUserIdIn(keycloakUserIds)
                .stream()
                .collect(Collectors.toMap(User::getKeycloakUserId, Function.identity()));

        content.forEach(user -> applyProfileImage(user, userByKeycloakId.get(user.getId())));
        return response;
    }

    @Transactional(readOnly = true)
    public KeycloakUserResponseDto getKeycloakUserById(String id) {
        KeycloakUserResponseDto user = keycloakAdminService.getUserById(id);
        User localUser = userRepository.findByKeycloakUserId(id).orElse(null);
        applyProfileImage(user, localUser);
        return user;
    }

    @Transactional
    public User updateUserApproval(UUID id, User.UserStatus status, String currentUserId) {
        User updatedUser = userApprobationService.updateStatus(id, status);
        String oldStatus = updatedUser.getStatus() == User.UserStatus.APPROVED ? "PENDING" : "APPROVED";

        if (updatedUser.getStatus() == User.UserStatus.APPROVED) {
            keycloakAdminService.enableUser(updatedUser.getKeycloakUserId());
        } else {
            keycloakAdminService.disableUser(updatedUser.getKeycloakUserId());
        }

        log.info("Estado de aprobacion actualizado para userId {} -> {}", id, updatedUser.getStatus());

        // Send Kafka event for email notification
        try {
            KeycloakUserResponseDto targetUser = keycloakAdminService.getUserById(updatedUser.getKeycloakUserId());
            KeycloakUserResponseDto adminUser = keycloakAdminService.getUserById(currentUserId);

            kafkaTemplate.send(
                    KafkaTopics.USER_STATUS_CHANGED_TOPIC,
                    UserStatusChangedEvent.builder()
                            .userId(updatedUser.getKeycloakUserId())
                            .username(targetUser.getUsername())
                            .email(targetUser.getEmail())
                            .firstName(targetUser.getFirstName())
                            .lastName(targetUser.getLastName())
                            .oldStatus(oldStatus)
                            .newStatus(updatedUser.getStatus().name())
                            .changedByUserId(currentUserId)
                            .changedByUsername(adminUser.getUsername())
                            .changedByEmail(adminUser.getEmail())
                            .changedByFirstName(adminUser.getFirstName())
                            .changedByLastName(adminUser.getLastName())
                            .reason("Status approval change")
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
            log.info("Evento de cambio de estado enviado: usuario={}, estado={}", updatedUser.getKeycloakUserId(), updatedUser.getStatus());
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de cambio de estado para usuario {}: {}",
                    updatedUser.getKeycloakUserId(), kafkaEx.getMessage());
        }

        return updatedUser;
    }

    @Transactional
    public CreateManagedUserResponseDto createManagedUser(CreateManagedUserRequestDto request, String currentUserId) {
        KeycloakUserResponseDto currentUser = keycloakAdminService.getUserById(currentUserId);
        String keycloakUserId = keycloakAdminService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                false
        );
        userApprobationService.createInvitedUser(keycloakUserId);
        String token = generateInvitationToken(keycloakUserId);
        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_INVITED_TOPIC,
                    UserInvitedEvent.builder()
                            .userId(keycloakUserId)
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .invitationToken(token)
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
            kafkaTemplate.send(
                    KafkaTopics.MANAGED_USER_CREATED_TOPIC,
                    ManagedUserCreatedEvent.builder()
                            .userId(keycloakUserId)
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .createdByUserId(currentUser.getId())
                            .createdByUsername(currentUser.getUsername())
                            .createdByEmail(currentUser.getEmail())
                            .createdByFirstName(currentUser.getFirstName())
                            .createdByLastName(currentUser.getLastName())
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de invitación para usuario {}: {}",
                    request.getUsername(), kafkaEx.getMessage());
        }
        return CreateManagedUserResponseDto.builder()
                .userId(keycloakUserId)
                .username(request.getUsername())
                .email(request.getEmail())
                .status(User.UserStatus.PENDING.name())
                .build();
    }

    private String generateInvitationToken(String keycloakUserId) {
        String token = UUID.randomUUID().toString();
        invitationTokenRepository.save(
                InvitationToken.builder()
                        .keycloakUserId(keycloakUserId)
                        .token(token)
                        .expiresAt(Instant.now().plusSeconds(86400)) // 24h
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

        keycloakAdminService.setPasswordAndEnable(invitation.getKeycloakUserId(), newPassword);
        invitation.setUsed(true);
        invitationTokenRepository.save(invitation);
        userApprobationService.activateUser(invitation.getKeycloakUserId());
        passwordPolicyService.recordPasswordChange(invitation.getKeycloakUserId());
        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_PASSWORD_CONFIGURED_TOPIC,
                    UserPasswordConfiguredEvent.builder()
                            .keycloakUserId(invitation.getKeycloakUserId())
                            .timestamp(System.currentTimeMillis())
                            .build()
            );
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento UserPasswordConfigured para userId={}: {}",
                    invitation.getKeycloakUserId(), kafkaEx.getMessage());
        }
        log.info("Usuario {} activó su cuenta via invitación", invitation.getKeycloakUserId());
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
            log.warn("No se pudo reenviar invitación para usuario {}: {}", keycloakUserId, kafkaEx.getMessage());
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

        KeycloakUserResponseDto user = keycloakAdminService.getUserById(invitation.getKeycloakUserId());

        return InvitationInfoResponseDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    @Transactional
    public void assignRoleToUser(String userId, String roleId, String currentUserId) {
        validateAssociatedCompanyForRole(userId, roleId);
        
        // Get user and admin info before making changes
        KeycloakUserResponseDto targetUser = keycloakAdminService.getUserById(userId);
        KeycloakUserResponseDto adminUser = keycloakAdminService.getUserById(currentUserId);
        String roleName = keycloakAdminService.getRoleNameById(roleId);
        
        // Assign the role
        keycloakAdminService.assignRoleToUser(userId, roleId);
        
        // Send Kafka event for email notification
        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_ROLE_ASSIGNED_TOPIC,
                    UserRoleAssignedEvent.builder()
                            .userId(userId)
                            .username(targetUser.getUsername())
                            .email(targetUser.getEmail())
                            .firstName(targetUser.getFirstName())
                            .lastName(targetUser.getLastName())
                            .roleId(roleId)
                            .roleName(roleName)
                            .assignedByUserId(currentUserId)
                            .assignedByUsername(adminUser.getUsername())
                            .assignedByEmail(adminUser.getEmail())
                            .assignedByFirstName(adminUser.getFirstName())
                            .assignedByLastName(adminUser.getLastName())
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
            log.info("Evento de asignación de rol enviado: usuario={}, rol={}, asignadoPor={}",
                    userId, roleName, currentUserId);
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de asignación de rol para usuario {}: {}",
                    userId, kafkaEx.getMessage());
        }
    }

    private void validateAssociatedCompanyForRole(String userId, String roleId) {
        String roleName = keycloakAdminService.getRoleNameById(roleId);

        List<String> privileges = keycloakAdminService.getRoleComposites(roleName).stream()
                .map(RoleResponseDto::getName)
                .toList();

        boolean grantsSolicitar = SOLICITAR_MANTENIMIENTO.equals(roleName)
                || privileges.contains(SOLICITAR_MANTENIMIENTO);
        boolean grantsSeleccionarEmpresa = SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO.equals(roleName)
                || privileges.contains(SELECCIONAR_EMPRESA_EN_SOLICITUD_MANTENIMIENTO);

        if (grantsSolicitar && !grantsSeleccionarEmpresa && !maintenanceCompanyClient.userHasCompany(userId)) {
            throw KeycloakException.requiresAssociatedCompany();
        }
    }

    public void removeRoleFromUser(String userId, String roleId) {
        keycloakAdminService.removeRoleFromUser(userId, roleId);
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getRolesByUserId(String userId) {
        return keycloakAdminService.getRolesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableStatuses() {
        return Arrays.stream(User.UserStatus.values())
                .map(Enum::name)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KeycloakUserResponseDto> getKeycloakUsersByIds(List<String> ids) {
        List<KeycloakUserResponseDto> users = keycloakAdminService.getUsersByIds(ids);
        if (users.isEmpty()) {
            return users;
        }

        List<String> keycloakUserIds = users.stream()
                .map(KeycloakUserResponseDto::getId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        Map<String, User> userByKeycloakId = userRepository.findAllByKeycloakUserIdIn(keycloakUserIds)
                .stream()
                .collect(Collectors.toMap(User::getKeycloakUserId, Function.identity()));

        users.forEach(user -> applyProfileImage(user, userByKeycloakId.get(user.getId())));
        return users;
    }

    @Transactional
    public KeycloakUserResponseDto updateCurrentUserProfileImage(String currentUserId, String objectName) {
        User localUser = userRepository.findByKeycloakUserId(currentUserId)
                .orElseThrow(() -> UserException.notFound(currentUserId));

        String normalizedObjectName = normalizeObjectName(objectName);
        if (normalizedObjectName == null) {
            throw new IllegalArgumentException("El nombre del objeto es obligatorio");
        }

        localUser.setProfileImageObjectName(normalizedObjectName);
        userRepository.save(localUser);

        KeycloakUserResponseDto user = keycloakAdminService.getUserById(currentUserId);
        applyProfileImage(user, localUser);
        return user;
    }

    private void applyProfileImage(KeycloakUserResponseDto user, User localUser) {
        if (user == null || localUser == null) {
            return;
        }
        String objectName = normalizeObjectName(localUser.getProfileImageObjectName());
        user.setProfileImageObjectName(objectName);
        user.setProfileImageUrl(toArchiveImageUrl(objectName));
    }

    private String toArchiveImageUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return ARCHIVE_FILE_ROUTE_PREFIX + objectName;
    }

    private String normalizeObjectName(String objectName) {
        if (objectName == null) {
            return null;
        }
        String trimmed = objectName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}