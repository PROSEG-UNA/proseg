package com.sssi.msvc_auth.repository;

import com.sssi.msvc_auth.entity.PasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, UUID> {

    Optional<PasswordPolicy> findByKeycloakUserId(String keycloakUserId);

    List<PasswordPolicy> findByExpiresAtBetween(
            Instant start,
            Instant end
    );
}