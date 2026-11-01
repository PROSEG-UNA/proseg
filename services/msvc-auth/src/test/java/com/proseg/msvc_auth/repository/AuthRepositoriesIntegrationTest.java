package com.proseg.msvc_auth.repository;

import com.proseg.msvc_auth.entity.InvitationToken;
import com.proseg.msvc_auth.entity.PasswordPolicy;
import com.proseg.msvc_auth.entity.PasswordResetToken;
import com.proseg.msvc_auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:auth-repository-tests;DB_CLOSE_DELAY=-1",
        "spring.cloud.config.enabled=false"
})
class AuthRepositoriesIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private InvitationTokenRepository invitationTokenRepository;
    @Autowired private PasswordPolicyRepository passwordPolicyRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void persistsUsersAndFindsByKeycloakId() {
        String keycloakId = UUID.randomUUID().toString();
        User user = User.builder().id(UUID.fromString(keycloakId))
                .keycloakUserId(keycloakId).status(User.UserStatus.PENDING).build();
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByKeycloakUserId(keycloakId))
                .get().extracting(User::getStatus).isEqualTo(User.UserStatus.PENDING);
        assertThat(userRepository.findAllByKeycloakUserIdIn(List.of(keycloakId, "missing")))
                .extracting(User::getKeycloakUserId).containsExactly(keycloakId);
    }

    @Test
    void findsActiveInvitationTokensAndInvalidatesThem() {
        Instant now = Instant.now();
        InvitationToken active = invitationTokenRepository.save(InvitationToken.builder()
                .keycloakUserId("user-a").token("invite-active").expiresAt(now.plusSeconds(60)).used(false).build());
        invitationTokenRepository.save(InvitationToken.builder()
                .keycloakUserId("user-a").token("invite-expired").expiresAt(now.minusSeconds(60)).used(false).build());
        invitationTokenRepository.save(InvitationToken.builder()
                .keycloakUserId("user-b").token("invite-other").expiresAt(now.plusSeconds(60)).used(true).build());
        invitationTokenRepository.flush();

        assertThat(invitationTokenRepository.findByTokenAndUsedFalse("invite-active")).contains(active);
        assertThat(invitationTokenRepository.findAllByUsedFalseAndExpiresAtAfter(now))
                .extracting(InvitationToken::getToken).containsExactly("invite-active");
        invitationTokenRepository.invalidateAllByKeycloakUserId("user-a");
        invitationTokenRepository.flush();
        assertThat(invitationTokenRepository.findByTokenAndUsedFalse("invite-active")).isEmpty();
    }

    @Test
    void queriesPasswordPoliciesByUserAndExpirationWindow() {
        Instant now = Instant.now();
        PasswordPolicy soon = passwordPolicyRepository.save(PasswordPolicy.builder()
                .keycloakUserId("user-a").lastChangedAt(now.minusSeconds(10))
                .expiresAt(now.plusSeconds(60)).policyDays(90).build());
        passwordPolicyRepository.save(PasswordPolicy.builder()
                .keycloakUserId("user-b").lastChangedAt(now.minusSeconds(10))
                .expiresAt(now.plusSeconds(3600)).policyDays(90).build());
        passwordPolicyRepository.flush();

        assertThat(passwordPolicyRepository.findByKeycloakUserId("user-a")).contains(soon);
        assertThat(passwordPolicyRepository.findByExpiresAtBetween(now, now.plusSeconds(120)))
                .extracting(PasswordPolicy::getKeycloakUserId).containsExactly("user-a");
    }

    @Test
    void findsUnusedResetTokensAndInvalidatesPriorTokens() {
        PasswordResetToken active = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .keycloakUserId("user-a").token("reset-active")
                .expiresAt(Instant.now().plusSeconds(60)).used(false).build());
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .keycloakUserId("user-a").token("reset-used")
                .expiresAt(Instant.now().plusSeconds(60)).used(true).build());
        passwordResetTokenRepository.flush();

        assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse("reset-active")).contains(active);
        assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse("reset-used")).isEmpty();
        passwordResetTokenRepository.invalidateAllByKeycloakUserId("user-a");
        passwordResetTokenRepository.flush();
        assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse("reset-active")).isEmpty();
    }
}
