package com.proseg.msvc_auth.scheduler;

import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.entity.PasswordPolicy;
import com.proseg.msvc_auth.service.KeycloakAdminService;
import com.proseg.msvc_auth.service.PasswordPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class PasswordPolicySchedulerTest {
    private final PasswordPolicyService policies = mock(PasswordPolicyService.class);
    private final KeycloakAdminService admin = mock(KeycloakAdminService.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final PasswordPolicyScheduler scheduler = new PasswordPolicyScheduler(policies, admin, kafka);

    @Test
    void notifiesOnlyConfiguredDaysAndContinuesAfterUserFailure() {
        when(policies.findPasswordsExpiringSoon(7)).thenReturn(List.of(
                policy("notify", Instant.now().plusSeconds(3 * 86400L + 3600)),
                policy("skip", Instant.now().plusSeconds(2 * 86400L + 3600)),
                policy("failure", Instant.now().plusSeconds(86400L + 3600))));
        when(policies.shouldSendExpirationNotification(anyLong())).thenAnswer(invocation ->
                invocation.getArgument(0, Long.class) != 2);
        when(admin.getUserById("notify")).thenReturn(KeycloakUserResponseDto.builder()
                .email("person@example.test").firstName("Test").build());
        when(admin.getUserById("failure")).thenThrow(new IllegalStateException("fixture failure"));

        scheduler.notifyExpiringPasswords();

        verify(kafka, times(1)).send(anyString(), any(Object.class));
        verify(admin, never()).getUserById("skip");
    }

    private PasswordPolicy policy(String userId, Instant expiresAt) {
        return PasswordPolicy.builder().keycloakUserId(userId).expiresAt(expiresAt).build();
    }
}
