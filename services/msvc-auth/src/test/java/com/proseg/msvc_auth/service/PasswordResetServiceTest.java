package com.proseg.msvc_auth.service;

import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.entity.PasswordResetToken;
import com.proseg.msvc_auth.exception.PasswordChangeException;
import com.proseg.msvc_auth.exception.PasswordResetException;
import com.proseg.msvc_auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetServiceTest {
    private final KeycloakAdminService admin = mock(KeycloakAdminService.class);
    private final KeycloakAuthService auth = mock(KeycloakAuthService.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordPolicyService policy = mock(PasswordPolicyService.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final PasswordResetService service =
            new PasswordResetService(admin, auth, tokenRepository, policy, kafka);

    @Test
    void resetUsesValidTokenAndRecordsPasswordChange() {
        PasswordResetToken token = PasswordResetToken.builder().keycloakUserId("kc-user")
                .token("reset-fixture").expiresAt(Instant.now().plusSeconds(120)).used(false).build();
        when(tokenRepository.findByTokenAndUsedFalse("reset-fixture")).thenReturn(Optional.of(token));

        service.resetPassword("reset-fixture", "NewPass123!");

        verify(admin).resetPassword("kc-user", "NewPass123!");
        verify(policy).recordPasswordChange("kc-user");
        verify(tokenRepository).save(argThat(PasswordResetToken::isUsed));
        verify(kafka).send(anyString(), any(Object.class));
    }

    @Test
    void resetRejectsUnknownAndExpiredTokens() {
        when(tokenRepository.findByTokenAndUsedFalse("missing")).thenReturn(Optional.empty());
        when(tokenRepository.findByTokenAndUsedFalse("expired")).thenReturn(Optional.of(
                PasswordResetToken.builder().keycloakUserId("kc-user").token("expired")
                        .expiresAt(Instant.now().minusSeconds(1)).used(false).build()));

        assertThatThrownBy(() -> service.resetPassword("missing", "NewPass123!"))
                .isInstanceOf(PasswordResetException.class);
        assertThatThrownBy(() -> service.resetPassword("expired", "NewPass123!"))
                .isInstanceOf(PasswordResetException.class);
        verifyNoInteractions(admin);
    }

    @Test
    void passwordChangeValidatesConfirmationBeforeAuthenticatingAndUpdatesPassword() {
        assertThatThrownBy(() -> service.changePassword("kc-user", "Current123!", "NewPass123!", "OtherPass123!"))
                .isInstanceOf(PasswordChangeException.class);
        assertThatThrownBy(() -> service.changePassword("kc-user", "SamePass123!", "SamePass123!", "SamePass123!"))
                .isInstanceOf(PasswordChangeException.class);
        verifyNoInteractions(admin, auth);

        when(admin.getUserById("kc-user")).thenReturn(KeycloakUserResponseDto.builder().username("alice").build());
        service.changePassword("kc-user", "Current123!", "NewPass123!", "NewPass123!");

        verify(auth).getToken("alice", "Current123!");
        verify(admin).resetPassword("kc-user", "NewPass123!");
        verify(policy).recordPasswordChange("kc-user");
    }

    @Test
    void resetRequestDoesNotDiscloseOrThrowForUnknownEmail() {
        when(admin.findUserIdByEmail("nobody@example.test"))
                .thenThrow(PasswordResetException.emailNotFound("nobody@example.test"));

        service.requestPasswordReset("nobody@example.test");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(kafka);
    }

    @Test
    void createsResetTokenAndPublishesNotificationForKnownEmail() {
        when(admin.findUserIdByEmail("person@example.test")).thenReturn("kc-user");
        when(admin.getUserById("kc-user")).thenReturn(KeycloakUserResponseDto.builder()
                .id("kc-user").firstName("Test").build());
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestPasswordReset("person@example.test");

        verify(tokenRepository).invalidateAllByKeycloakUserId("kc-user");
        verify(tokenRepository).save(argThat(token -> token.getKeycloakUserId().equals("kc-user")
                && token.getToken() != null
                && token.getExpiresAt().isAfter(Instant.now())));
        verify(kafka).send(anyString(), any(Object.class));
    }

    @Test
    void expiredPasswordResetFlowInvalidatesOlderTokensAndNotifiesUser() {
        when(admin.getUserById("kc-user")).thenReturn(KeycloakUserResponseDto.builder()
                .id("kc-user").email("person@example.test").firstName("Test").build());
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestExpiredPasswordReset("kc-user");

        verify(tokenRepository).invalidateAllByKeycloakUserId("kc-user");
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(kafka).send(anyString(), any(Object.class));
    }
}
