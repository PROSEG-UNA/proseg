package com.sssi.msvc_auth.repository;

import com.sssi.msvc_auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByKeycloakUserId(String keycloakUserId);
}

