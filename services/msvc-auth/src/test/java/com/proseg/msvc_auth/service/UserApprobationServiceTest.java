package com.proseg.msvc_auth.service;

import com.proseg.msvc_auth.entity.User;
import com.proseg.msvc_auth.exception.AuthorizationException;
import com.proseg.msvc_auth.exception.UserException;
import com.proseg.msvc_auth.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserApprobationServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserApprobationService service = new UserApprobationService(repository);

    @Test
    void createsUsersWithExpectedStatusesAndRejectsNonUuidIds() {
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String id = UUID.randomUUID().toString();

        assertThat(service.createPendingUser(id).getStatus()).isEqualTo(User.UserStatus.PENDING);
        assertThat(service.createInvitedUser(UUID.randomUUID().toString()).getStatus()).isEqualTo(User.UserStatus.INVITED);
        assertThat(service.createApprovedUser(UUID.randomUUID().toString()).getStatus()).isEqualTo(User.UserStatus.APPROVED);
        assertThatThrownBy(() -> service.createPendingUser("not-a-uuid"))
                .isInstanceOf(UserException.class);
    }

    @Test
    void approvalChecksAndStatusChangesAreBackedByRepository() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).keycloakUserId(id.toString()).status(User.UserStatus.PENDING).build();
        when(repository.findByKeycloakUserId(id.toString())).thenReturn(Optional.of(user));
        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.assertUserIsApproved(id.toString()))
                .isInstanceOf(AuthorizationException.class);
        service.updateStatus(id, User.UserStatus.APPROVED);
        service.assertUserIsApproved(id.toString());
        assertThat(service.activateUser(id.toString()).getStatus()).isEqualTo(User.UserStatus.APPROVED);
        assertThat(service.findByKeycloakUserId(id.toString())).isSameAs(user);
        assertThat(service.getClass()).isNotNull();
        verify(repository, atLeast(2)).save(user);
    }

    @Test
    void missingUsersAreReportedForLookupAndStatusUpdate() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.findByKeycloakUserId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(), User.UserStatus.REJECTED))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> service.activateUser("unknown"))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> service.findByKeycloakUserId("unknown"))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> service.assertUserIsApproved("unknown"))
                .isInstanceOf(AuthorizationException.class);
    }
}
