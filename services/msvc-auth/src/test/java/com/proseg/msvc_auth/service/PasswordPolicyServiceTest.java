package com.proseg.msvc_auth.service;

import com.proseg.msvc_auth.entity.PasswordPolicy;
import com.proseg.msvc_auth.exception.PasswordExpiredException;
import com.proseg.msvc_auth.repository.PasswordPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordPolicyServiceTest {
    private final PasswordPolicyRepository repository = mock(PasswordPolicyRepository.class);
    private final PasswordPolicyService service = new PasswordPolicyService(repository);

    @Test
    void recordsChangeForNewAndExistingPolicy() {
        ReflectionTestUtils.setField(service, "expirationDays", 30);
        when(repository.findByKeycloakUserId("new-user")).thenReturn(Optional.empty());
        when(repository.findByKeycloakUserId("existing-user")).thenReturn(Optional.of(
                PasswordPolicy.builder().keycloakUserId("existing-user").policyDays(90).build()));

        service.recordPasswordChange("new-user");
        service.recordPasswordChange("existing-user");

        verify(repository, times(2)).save(any(PasswordPolicy.class));
        verify(repository).save(argThat(policy -> policy.getKeycloakUserId().equals("new-user")
                && policy.getPolicyDays() == 30
                && policy.getExpiresAt().isAfter(Instant.now().plusSeconds(29L * 86400))));
    }

    @Test
    void onlyRejectsExpiredPoliciesAndQueriesNotificationWindow() {
        when(repository.findByKeycloakUserId("expired")).thenReturn(Optional.of(
                PasswordPolicy.builder().keycloakUserId("expired").expiresAt(Instant.now().minusSeconds(1)).build()));
        when(repository.findByKeycloakUserId("current")).thenReturn(Optional.of(
                PasswordPolicy.builder().keycloakUserId("current").expiresAt(Instant.now().plusSeconds(60)).build()));
        when(repository.findByExpiresAtBetween(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.assertPasswordNotExpired("expired"))
                .isInstanceOf(PasswordExpiredException.class);
        service.assertPasswordNotExpired("current");
        service.assertPasswordNotExpired("no-policy");
        assertThat(service.findPasswordsExpiringSoon(7)).isEmpty();
        verify(repository).findByExpiresAtBetween(any(), any());
    }

    @Test
    void parsesConfiguredNotificationDays() {
        ReflectionTestUtils.setField(service, "notificationDaysConfig", "7, 3,1,3");

        assertThat(service.shouldSendExpirationNotification(7)).isTrue();
        assertThat(service.shouldSendExpirationNotification(2)).isFalse();
    }
}
