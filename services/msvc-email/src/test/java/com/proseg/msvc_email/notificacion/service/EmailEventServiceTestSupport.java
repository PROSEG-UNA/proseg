package com.proseg.msvc_email.notificacion.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_email.notificacion.client.AuthClient;
import com.proseg.msvc_email.notificacion.client.MaintenanceClient;
import com.proseg.msvc_email.notificacion.dto.KeycloakUserResponseDto;
import com.proseg.msvc_email.notificacion.model.Email;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

final class EmailEventServiceTestSupport {

    static final String BRAND = "MARCA_TEST";
    static final String LOGIN_URL = "https://app.test/login";
    static final long TS = 1_700_000_000_000L;

    private EmailEventServiceTestSupport() {
    }

    static EmailEventService newService(
            EmailService emailService,
            AuthClient authClient,
            MaintenanceClient maintenanceClient
    ) {
        EmailEventService service =
                new EmailEventService(emailService, authClient, maintenanceClient);
        ReflectionTestUtils.setField(service, "loginUrl", LOGIN_URL);
        ReflectionTestUtils.setField(service, "approvalsBaseUrl", "https://app.test/approvals");
        ReflectionTestUtils.setField(service, "setPasswordBaseUrl", "https://app.test/set-password");
        ReflectionTestUtils.setField(service, "resetPasswordBaseUrl", "https://app.test/reset-password");
        ReflectionTestUtils.setField(service, "brandName", BRAND);
        return service;
    }

    static KeycloakUserResponseDto keycloakUser(
            String username, String email, String firstName, String lastName) {
        KeycloakUserResponseDto dto = new KeycloakUserResponseDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        return dto;
    }

    static <T> ApiResponse<T> apiResponse(T data) {
        return new ApiResponse<>("ok", data, 200);
    }

    static Email captureEmail(EmailService emailService) {
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());
        return captor.getValue();
    }

    static Email captureEmail(EmailService emailService, int invocationIndex) {
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService, times(invocationIndex)).sendEmail(captor.capture());
        return captor.getAllValues().get(invocationIndex - 1);
    }

    static List<KeycloakUserResponseDto> superAdminsIncludingNoise() {
        return List.of(
                keycloakUser("service-account-job", "sa@test.com", "SA", "Bot"),
                keycloakUser("admin-bcc", "bcc-admin@test.com", "Bcc", "Admin"),
                keycloakUser("admin-no-mail", null, "Sin", "Mail")
        );
    }
}
