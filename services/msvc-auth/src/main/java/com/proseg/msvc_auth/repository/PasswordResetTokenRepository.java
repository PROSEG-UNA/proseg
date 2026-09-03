package com.proseg.msvc_auth.repository;

import com.proseg.msvc_auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true " +
            "WHERE t.keycloakUserId = :keycloakUserId AND t.used = false")
    void invalidateAllByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);
}