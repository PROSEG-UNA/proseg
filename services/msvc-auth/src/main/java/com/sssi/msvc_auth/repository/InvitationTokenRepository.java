package com.sssi.msvc_auth.repository;

import com.sssi.msvc_auth.entity.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByTokenAndUsedFalse(String token);

    @Modifying
    @Query("UPDATE InvitationToken it SET it.used = true WHERE it.keycloakUserId = :keycloakUserId AND it.used = false")
    void invalidateAllByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);

    List<InvitationToken> findAllByUsedFalseAndExpiresAtAfter(Instant now);
}
