package com.sssi.msvc_auth.service;

import com.sssi.common.api.response.PagedResponse;
import com.sssi.common.kafka.events.UserAdminCreatedEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_auth.dto.CreateManagedUserRequestDto;
import com.sssi.msvc_auth.dto.CreateManagedUserResponseDto;
import com.sssi.msvc_auth.dto.KeycloakUserResponseDto;
import com.sssi.msvc_auth.dto.RoleResponseDto;
import com.sssi.msvc_auth.entity.User;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIALS = "!@#$%";
    private static final String PASSWORD_ALPHABET = UPPERCASE + LOWERCASE + DIGITS + SPECIALS;
    private static final int GENERATED_PASSWORD_LENGTH = 14;

    private final KeycloakAdminService keycloakAdminService;
    private final UserApprobationService userApprobationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public PagedResponse<KeycloakUserResponseDto> getAllUsers(Pageable pageable) {
    return keycloakAdminService.getAllUsers(pageable);
    }

    @Transactional(readOnly = true)
    public KeycloakUserResponseDto getKeycloakUserById(String id) {
        return keycloakAdminService.getUserById(id);
    }

    @Transactional
    public User updateUserApproval(UUID id, User.UserStatus status) {
        User updatedUser = userApprobationService.updateStatus(id, status);

        if (updatedUser.getStatus() == User.UserStatus.APPROVED) {
            keycloakAdminService.enableUser(updatedUser.getKeycloakUserId());
        } else {
            keycloakAdminService.disableUser(updatedUser.getKeycloakUserId());
        }

        log.info("Estado de aprobacion actualizado para userId {} -> {}", id, updatedUser.getStatus());
        return updatedUser;
    }

    @Transactional
    public CreateManagedUserResponseDto createManagedUser(CreateManagedUserRequestDto request) {
        String generatedPassword = generatePassword();

        String keycloakUserId = keycloakAdminService.registerUser(
                request.getUsername(),
                request.getEmail(),
                generatedPassword,
                request.getFirstName(),
                request.getLastName(),
                true
        );

        userApprobationService.createApprovedUser(keycloakUserId);

        try {
            kafkaTemplate.send(
                    KafkaTopics.USER_ADMIN_CREATED_TOPIC,
                    UserAdminCreatedEvent.builder()
                            .userId(keycloakUserId)
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .temporaryPassword(generatedPassword)
                            .timestamp(Instant.now().toEpochMilli())
                            .build()
            );
        } catch (Exception kafkaEx) {
            log.warn("No se pudo enviar evento de alta administrativa para usuario {}: {}",
                    request.getUsername(), kafkaEx.getMessage());
        }

        return CreateManagedUserResponseDto.builder()
                .userId(keycloakUserId)
                .username(request.getUsername())
                .email(request.getEmail())
                .status(User.UserStatus.APPROVED.name())
                .build();
    }

    public void assignRoleToUser(String userId, String roleId) {
        keycloakAdminService.assignRoleToUser(userId, roleId);
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

    private String generatePassword() {
        List<Character> chars = new ArrayList<>(GENERATED_PASSWORD_LENGTH);

        chars.add(randomChar(UPPERCASE));
        chars.add(randomChar(LOWERCASE));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SPECIALS));

        for (int i = chars.size(); i < GENERATED_PASSWORD_LENGTH; i++) {
            chars.add(randomChar(PASSWORD_ALPHABET));
        }

        Collections.shuffle(chars, secureRandom);

        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (Character c : chars) {
            password.append(c);
        }
        return password.toString();
    }

    private char randomChar(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }
}

