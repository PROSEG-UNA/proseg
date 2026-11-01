package com.proseg.msvc_auth.service;

import com.proseg.msvc_auth.client.MaintenanceCompanyClient;
import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.dto.RoleResponseDto;
import com.proseg.msvc_auth.entity.InvitationToken;
import com.proseg.msvc_auth.entity.User;
import com.proseg.msvc_auth.exception.InvitationException;
import com.proseg.msvc_auth.exception.KeycloakException;
import com.proseg.msvc_auth.repository.InvitationTokenRepository;
import com.proseg.msvc_auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final KeycloakAdminService admin = mock(KeycloakAdminService.class);
    private final UserApprobationService approval = mock(UserApprobationService.class);
    private final PasswordPolicyService passwordPolicy = mock(PasswordPolicyService.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final InvitationTokenRepository invitations = mock(InvitationTokenRepository.class);
    private final MaintenanceCompanyClient companyClient = mock(MaintenanceCompanyClient.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserService service =
            new UserService(admin, approval, passwordPolicy, kafka, invitations, companyClient, users);

    @Test
    void updatesLocalProfileImageAndUsesTheLocalRecordAsSourceOfTruth() {
        User localUser = User.builder().keycloakUserId("kc-user").profileImageObjectName("old.png").build();
        when(users.findByKeycloakUserId("kc-user")).thenReturn(Optional.of(localUser));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(admin.getUserById("kc-user")).thenReturn(KeycloakUserResponseDto.builder()
                .id("kc-user").username("alice").profileImageObjectName("stale.png").build());

        KeycloakUserResponseDto response = service.updateCurrentUserProfileImage("kc-user", "  avatars/user.png ");

        assertThat(localUser.getProfileImageObjectName()).isEqualTo("avatars/user.png");
        assertThat(response.getProfileImageObjectName()).isEqualTo("avatars/user.png");
        assertThat(response.getProfileImageUrl()).isEqualTo("/api/v1/archive/files/avatars/user.png");
        verify(users).save(localUser);
    }

    @Test
    void rejectsMissingUserAndBlankImageName() {
        when(users.findByKeycloakUserId("missing")).thenReturn(Optional.empty());
        when(users.findByKeycloakUserId("kc-user")).thenReturn(Optional.of(
                User.builder().keycloakUserId("kc-user").build()));

        assertThatThrownBy(() -> service.updateCurrentUserProfileImage("missing", "image.png"))
                .hasMessageContaining("missing");
        assertThatThrownBy(() -> service.updateCurrentUserProfileImage("kc-user", " "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(users, never()).save(any());
    }

    @Test
    void roleAssignmentRequiresAssociatedCompanyForMaintenanceRequesterRole() {
        when(admin.getRoleNameById("role-id")).thenReturn("SOLICITAR_MANTENIMIENTO");
        when(admin.getRoleComposites("SOLICITAR_MANTENIMIENTO")).thenReturn(List.of());
        when(companyClient.userHasCompany("kc-user")).thenReturn(false);

        assertThatThrownBy(() -> service.assignRoleToUser("kc-user", "role-id", "admin-id"))
                .isInstanceOf(KeycloakException.class)
                .hasMessageContaining("empresa asociada");
        verifyNoInteractions(kafka);
        verify(admin, never()).assignRoleToUser(anyString(), anyString());
    }

    @Test
    void activatesInvitationOnlyOnceAndRecordsPasswordChange() {
        InvitationToken token = InvitationToken.builder().keycloakUserId("kc-user")
                .token("invite-token").expiresAt(Instant.now().plusSeconds(120)).used(false).build();
        when(invitations.findByTokenAndUsedFalse("invite-token")).thenReturn(Optional.of(token));
        when(invitations.save(any(InvitationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.activateUserWithToken("invite-token", "NewPass123!");

        assertThat(token.isUsed()).isTrue();
        verify(admin).setPasswordAndEnable("kc-user", "NewPass123!");
        verify(approval).activateUser("kc-user");
        verify(passwordPolicy).recordPasswordChange("kc-user");
        verify(kafka).send(anyString(), any(Object.class));
    }

    @Test
    void invitationLookupRejectsExpiredAndUnknownTokens() {
        when(invitations.findByTokenAndUsedFalse("unknown")).thenReturn(Optional.empty());
        when(invitations.findByTokenAndUsedFalse("expired")).thenReturn(Optional.of(
                InvitationToken.builder().keycloakUserId("kc-user").token("expired")
                        .expiresAt(Instant.now().minusSeconds(1)).used(false).build()));

        assertThatThrownBy(() -> service.getInvitationInfo("unknown")).isInstanceOf(InvitationException.class);
        assertThatThrownBy(() -> service.getInvitationInfo("expired")).isInstanceOf(InvitationException.class);
        verifyNoInteractions(admin);
    }

    @Test
    void returnsAvailableStatusesFromTheEntityEnum() {
        assertThat(service.getAvailableStatuses())
                .containsExactly("PENDING", "APPROVED", "REJECTED", "INVITED");
    }

    @Test
    void createsManagedUserInvitationAndResendsItOnlyForInvitedUsers() {
        when(admin.getUserById("admin-id")).thenReturn(KeycloakUserResponseDto.builder()
                .id("admin-id").username("admin").email("admin@example.test").build());
        when(admin.registerUser("new-user", "new@example.test", "New", "User", false)).thenReturn("new-id");
        when(invitations.save(any(InvitationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(admin.getUserById("new-id")).thenReturn(KeycloakUserResponseDto.builder()
                .id("new-id").username("new-user").email("new@example.test").build());
        when(approval.findByKeycloakUserId("new-id")).thenReturn(User.builder()
                .keycloakUserId("new-id").status(User.UserStatus.INVITED).build());

        var response = service.createManagedUser(
                new com.proseg.msvc_auth.dto.CreateManagedUserRequestDto(
                        "new-user", "new@example.test", "New", "User"), "admin-id");
        service.resendInvitation("new-id");

        assertThat(response.getUserId()).isEqualTo("new-id");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(approval).createInvitedUser("new-id");
        verify(invitations).invalidateAllByKeycloakUserId("new-id");
        verify(invitations, times(2)).save(argThat(token -> token.getExpiresAt().isAfter(Instant.now())));
        verify(kafka, atLeast(2)).send(anyString(), any(Object.class));
        when(approval.findByKeycloakUserId("active-id")).thenReturn(User.builder()
                .keycloakUserId("active-id").status(User.UserStatus.APPROVED).build());
        assertThatThrownBy(() -> service.resendInvitation("active-id"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approvalAndRoleOperationsCoordinateKeycloakAndEvents() {
        UUID userId = UUID.randomUUID();
        User approved = User.builder().id(userId).keycloakUserId("target-id")
                .status(User.UserStatus.APPROVED).build();
        when(approval.updateStatus(userId, User.UserStatus.APPROVED)).thenReturn(approved);
        when(admin.getUserById("target-id")).thenReturn(KeycloakUserResponseDto.builder()
                .id("target-id").username("target").email("target@example.test").build());
        when(admin.getUserById("admin-id")).thenReturn(KeycloakUserResponseDto.builder()
                .id("admin-id").username("admin").email("admin@example.test").build());
        when(admin.getRoleNameById("role-id")).thenReturn("VIEW_REPORTS");
        when(admin.getRoleComposites("VIEW_REPORTS")).thenReturn(List.of(
                RoleResponseDto.builder().name("VIEW_REPORTS").build()));

        service.updateUserApproval(userId, User.UserStatus.APPROVED, "admin-id");
        service.assignRoleToUser("target-id", "role-id", "admin-id");
        service.removeRoleFromUser("target-id", "role-id");

        verify(admin).enableUser("target-id");
        verify(admin).assignRoleToUser("target-id", "role-id");
        verify(admin).removeRoleFromUser("target-id", "role-id");
        verify(companyClient, never()).userHasCompany(anyString());
        verify(kafka, atLeast(2)).send(anyString(), any(Object.class));
    }

    @Test
    void enrichesBulkAndPagedKeycloakUsersFromLocalImageState() {
        KeycloakUserResponseDto kcUser = KeycloakUserResponseDto.builder().id("kc-1").username("alice").build();
        when(admin.getAllUsers(any())).thenReturn(com.proseg.common.api.response.PagedResponse
                .<KeycloakUserResponseDto>builder().content(List.of(kcUser)).page(0).size(1).build());
        when(admin.getUsersByIds(List.of("kc-1"))).thenReturn(List.of(kcUser));
        User local = User.builder().keycloakUserId("kc-1").profileImageObjectName("avatar.jpg").build();
        when(users.findAllByKeycloakUserIdIn(anyList())).thenReturn(List.of(local));

        var page = service.getAllUsers(org.springframework.data.domain.PageRequest.of(0, 1));
        var bulk = service.getKeycloakUsersByIds(List.of("kc-1"));

        assertThat(page.content().get(0).getProfileImageUrl()).isEqualTo("/api/v1/archive/files/avatar.jpg");
        assertThat(bulk.get(0).getProfileImageObjectName()).isEqualTo("avatar.jpg");
        when(admin.getUsersByIds(List.of())).thenReturn(List.of());
        assertThat(service.getKeycloakUsersByIds(List.of())).isEmpty();
    }

    @Test
    void returnsInvitationInfoAndRejectsExpiredActivationTokens() {
        Instant expiry = Instant.now().plusSeconds(120);
        InvitationToken active = InvitationToken.builder().keycloakUserId("kc-user")
                .token("active-token").expiresAt(expiry).used(false).build();
        when(invitations.findByTokenAndUsedFalse("active-token")).thenReturn(Optional.of(active));
        when(admin.getUserById("kc-user")).thenReturn(KeycloakUserResponseDto.builder()
                .firstName("Alice").lastName("User").email("alice@example.test").build());
        when(invitations.findByTokenAndUsedFalse("expired-token")).thenReturn(Optional.of(
                InvitationToken.builder().keycloakUserId("kc-user").token("expired-token")
                        .expiresAt(Instant.now().minusSeconds(1)).used(false).build()));

        var result = service.getInvitationInfo("active-token");

        assertThat(result.getEmail()).isEqualTo("alice@example.test");
        assertThat(result.getFirstName()).isEqualTo("Alice");
        assertThat(result.getExpiresAt()).isEqualTo(expiry);
        assertThatThrownBy(() -> service.activateUserWithToken("expired-token", "NewPass123!"))
                .isInstanceOf(InvitationException.class);
        verify(admin, never()).setPasswordAndEnable(anyString(), anyString());
    }

    @Test
    void rejectedApprovalDisablesKeycloakAndEmptyUserPageSkipsLocalImageLookup() {
        UUID userId = UUID.randomUUID();
        User rejected = User.builder().id(userId).keycloakUserId("target-id")
                .status(User.UserStatus.REJECTED).build();
        when(approval.updateStatus(userId, User.UserStatus.REJECTED)).thenReturn(rejected);
        when(admin.getUserById("target-id")).thenReturn(KeycloakUserResponseDto.builder().build());
        when(admin.getUserById("admin-id")).thenReturn(KeycloakUserResponseDto.builder().build());
        when(admin.getAllUsers(any())).thenReturn(com.proseg.common.api.response.PagedResponse
                .<KeycloakUserResponseDto>builder().content(List.of()).build());

        service.updateUserApproval(userId, User.UserStatus.REJECTED, "admin-id");
        service.getAllUsers(org.springframework.data.domain.PageRequest.of(0, 10));

        verify(admin).disableUser("target-id");
        verify(users, never()).findAllByKeycloakUserIdIn(anyList());
        verify(kafka).send(anyString(), any(Object.class));
    }
}
