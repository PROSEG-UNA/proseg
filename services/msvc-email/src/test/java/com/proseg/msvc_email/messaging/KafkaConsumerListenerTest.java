package com.proseg.msvc_email.messaging;

import com.proseg.common.kafka.events.*;
import com.proseg.msvc_email.notificacion.service.EmailEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerListenerTest {

    @Mock
    private EmailEventService emailEventService;

    private KafkaConsumerListener listener;

    @BeforeEach
    void setUp() {
        listener = new KafkaConsumerListener(emailEventService);
    }

    @Test
    @DisplayName("onUserLogin delega en sendLoginEmail")
    void onUserLogin_cuandoEvento_delegaEnSendLoginEmail() {
        UserLoginEvent event = mock(UserLoginEvent.class);

        listener.onUserLogin(event);

        verify(emailEventService).sendLoginEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onUserRegistered delega en bienvenida y aprobaciones")
    void onUserRegistered_cuandoEvento_delegaEnRegistroYAprobacion() {
        UserRegisteredEvent event = mock(UserRegisteredEvent.class);

        listener.onUserRegistered(event);

        verify(emailEventService).sendRegisteredEmail(event);
        verify(emailEventService).sendApprovalEmails(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onUserInvited delega en sendInvitationEmail")
    void onUserInvited_cuandoEvento_delegaEnInvitacion() {
        UserInvitedEvent event = mock(UserInvitedEvent.class);

        listener.onUserInvited(event);

        verify(emailEventService).sendInvitationEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onManagedUserCreated delega en notificación a admins")
    void onManagedUserCreated_cuandoEvento_delegaEnNotificacionAdmin() {
        ManagedUserCreatedEvent event = mock(ManagedUserCreatedEvent.class);

        listener.onManagedUserCreated(event);

        verify(emailEventService).sendManagedUserCreatedNotificationEmails(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onUserPasswordConfigured delega en sendPasswordConfiguredEmail")
    void onUserPasswordConfigured_cuandoEvento_delegaEnPasswordConfigurada() {
        UserPasswordConfiguredEvent event = mock(UserPasswordConfiguredEvent.class);

        listener.onUserPasswordConfigured(event);

        verify(emailEventService).sendPasswordConfiguredEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onPasswordResetRequested delega en sendPasswordResetEmail")
    void onPasswordResetRequested_cuandoEvento_delegaEnResetPassword() {
        PasswordResetRequestedEvent event = mock(PasswordResetRequestedEvent.class);

        listener.onPasswordResetRequested(event);

        verify(emailEventService).sendPasswordResetEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onPasswordChanged delega en sendPasswordChangedEmail")
    void onPasswordChanged_cuandoEvento_delegaEnPasswordCambiada() {
        PasswordChangedEvent event = mock(PasswordChangedEvent.class);

        listener.onPasswordChanged(event);

        verify(emailEventService).sendPasswordChangedEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onPasswordExpiringSoon delega en sendPasswordExpiringSoonEmail")
    void onPasswordExpiringSoon_cuandoEvento_delegaEnExpiracionProxima() {
        PasswordExpiringSoonEvent event = mock(PasswordExpiringSoonEvent.class);

        listener.onPasswordExpiringSoon(event);

        verify(emailEventService).sendPasswordExpiringSoonEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onPasswordExpiredResetRequested delega en sendPasswordExpiredResetEmail")
    void onPasswordExpiredResetRequested_cuandoEvento_delegaEnResetPorExpiracion() {
        PasswordExpiredResetRequiredEvent event = mock(PasswordExpiredResetRequiredEvent.class);

        listener.onPasswordExpiredResetRequested(event);

        verify(emailEventService).sendPasswordExpiredResetEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onCompanyUsersAssigned delega en sendCompanyUsersAssignedEmails")
    void onCompanyUsersAssigned_cuandoEvento_delegaEnAsignacionEmpresa() {
        CompanyUsersAssignedEvent event = mock(CompanyUsersAssignedEvent.class);

        listener.onCompanyUsersAssigned(event);

        verify(emailEventService).sendCompanyUsersAssignedEmails(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onMaintenanceRequestCreated delega en sendMaintenanceRequestCreatedEmail")
    void onMaintenanceRequestCreated_cuandoEvento_delegaEnSolicitudCreada() {
        MaintenanceRequestCreatedEvent event = mock(MaintenanceRequestCreatedEvent.class);

        listener.onMaintenanceRequestCreated(event);

        verify(emailEventService).sendMaintenanceRequestCreatedEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onMaintenanceRequestNotification delega en sendMaintenanceRequestNotificationEmail")
    void onMaintenanceRequestNotification_cuandoEvento_delegaEnNotificacionMantenimiento() {
        MaintenanceRequestNotificationEvent event = mock(MaintenanceRequestNotificationEvent.class);

        listener.onMaintenanceRequestNotification(event);

        verify(emailEventService).sendMaintenanceRequestNotificationEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onTicketNotification delega en sendTicketNotificationEmail")
    void onTicketNotification_cuandoEvento_delegaEnNotificacionTicket() {
        TicketNotificationEvent event = mock(TicketNotificationEvent.class);

        listener.onTicketNotification(event);

        verify(emailEventService).sendTicketNotificationEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onUserRoleAssigned delega en sendUserRoleAssignedEmail")
    void onUserRoleAssigned_cuandoEvento_delegaEnRolAsignado() {
        UserRoleAssignedEvent event = mock(UserRoleAssignedEvent.class);

        listener.onUserRoleAssigned(event);

        verify(emailEventService).sendUserRoleAssignedEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }

    @Test
    @DisplayName("onUserStatusChanged delega en sendUserStatusChangedEmail")
    void onUserStatusChanged_cuandoEvento_delegaEnEstadoCambiado() {
        UserStatusChangedEvent event = mock(UserStatusChangedEvent.class);

        listener.onUserStatusChanged(event);

        verify(emailEventService).sendUserStatusChangedEmail(event);
        verifyNoMoreInteractions(emailEventService);
    }
}
