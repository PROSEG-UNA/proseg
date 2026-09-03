package com.proseg.msvc_auth.service;

import com.proseg.msvc_auth.entity.User;
import com.proseg.msvc_auth.exception.AuthorizationException;
import com.proseg.msvc_auth.exception.UserException;
import com.proseg.msvc_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserApprobationService {

    private final UserRepository userRepository;

    @Transactional
    public User createPendingUser(String keycloakUserId) {
        return createUserWithStatus(keycloakUserId, User.UserStatus.PENDING);
    }

    @Transactional
    public User createInvitedUser(String keycloakUserId) {
        return createUserWithStatus(keycloakUserId, User.UserStatus.INVITED);
    }

    @Transactional
    public User createApprovedUser(String keycloakUserId) {
        return createUserWithStatus(keycloakUserId, User.UserStatus.APPROVED);
    }

    @Transactional
    public User updateStatus(UUID id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> UserException.notFound(id.toString()));

        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void assertUserIsApproved(String keycloakUserId) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> AuthorizationException.accountNotApproved(User.UserStatus.PENDING.name()));

        if (user.getStatus() != User.UserStatus.APPROVED) {
            throw AuthorizationException.accountNotApproved(user.getStatus().name());
        }
    }

    private User createUserWithStatus(String keycloakUserId, User.UserStatus status) {
        UUID sharedUserId;
        try {
            sharedUserId = UUID.fromString(keycloakUserId);
        } catch (IllegalArgumentException ex) {
            throw UserException.invalidUserIdFormat(keycloakUserId);
        }

        User user = User.builder()
                .id(sharedUserId)
                .keycloakUserId(keycloakUserId)
                .status(status)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User activateUser(String keycloakUserId) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> UserException.notFound(keycloakUserId));
        user.setStatus(User.UserStatus.APPROVED);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByKeycloakUserId(String keycloakUserId) {
        return userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> UserException.notFound(keycloakUserId));
    }
}

