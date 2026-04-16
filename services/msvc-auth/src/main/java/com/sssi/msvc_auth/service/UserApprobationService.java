package com.sssi.msvc_auth.service;

import com.sssi.msvc_auth.entity.User;
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
        User user = User.builder()
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
}

