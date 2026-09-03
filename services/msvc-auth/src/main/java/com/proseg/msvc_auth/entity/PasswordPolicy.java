package com.proseg.msvc_auth.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_policy_table")
public class PasswordPolicy extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private String keycloakUserId;

    @Column(name = "last_changed_at", nullable = false)
    private Instant lastChangedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "policy_days", nullable = false)
    private int policyDays;
}