package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.proseg.msvc_maintenance.entity.Ticket;
import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

final class TicketServiceImplTestSupport {

    static final String CREATOR_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    static final String OTHER_USER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    static final String ADMIN_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";

    private TicketServiceImplTestSupport() {
    }

    static void initService(TicketServiceImpl service) {
        ReflectionTestUtils.setField(service, "ticketOverdueHours", 72L);
        ReflectionTestUtils.setField(service, "archiveBaseUrl", "http://localhost:8081");
    }

    static Ticket baseTicket(UUID ticketId) {
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .title("Falla AC")
                .description("No enfría")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .build();
        ticket.setCreatedBy(CREATOR_ID);
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.now());
        return ticket;
    }

    static JwtAuthenticationToken jwtAuth(String subject, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();
        var granted = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new JwtAuthenticationToken(jwt, granted);
    }

    static KeycloakUserResponse keycloakUser(String id, String firstName, String lastName) {
        return new KeycloakUserResponse(id, "user", id + "@acme.com", firstName, lastName, "ACTIVE", List.of());
    }

    static void stubAuthorNames(AuthClient authClient, KeycloakUserResponse... users) {
        org.mockito.Mockito.lenient()
                .when(authClient.findUsersByKeycloakIds(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new ApiResponse<>("ok", List.of(users), 200));
    }
}
