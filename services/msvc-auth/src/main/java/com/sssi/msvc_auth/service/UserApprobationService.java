package com.sssi.msvc_auth.service;

import com.sssi.msvc_auth.entity.User;
import com.sssi.msvc_auth.exception.AuthorizationException;
import com.sssi.msvc_auth.exception.UserException;
import com.sssi.msvc_auth.repository.UserRepository;
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
        UUID sharedUserId;
        try {
            sharedUserId = UUID.fromString(keycloakUserId);
        } catch (IllegalArgumentException ex) {
            throw UserException.invalidUserIdFormat(keycloakUserId);
        }

        User user = User.builder()
                .id(sharedUserId)
                .keycloakUserId(keycloakUserId)
                .status(User.UserStatus.PENDING)
                .build();

        return userRepository.save(user);
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
}

