package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PagedResponse;
import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.proseg.msvc_maintenance.dto.request.TicketAssignedToUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketStatusUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.proseg.msvc_maintenance.entity.Ticket;
import com.proseg.msvc_maintenance.entity.TicketHistoryChange;
import com.proseg.msvc_maintenance.entity.enums.TicketHistoryChangeType;
import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import com.proseg.msvc_maintenance.event.TicketNotificationDomainEvent;
import com.proseg.msvc_maintenance.exception.TicketException;
import com.proseg.msvc_maintenance.repository.TicketAssetRepository;
import com.proseg.msvc_maintenance.repository.TicketCommentRepository;
import com.proseg.msvc_maintenance.repository.TicketHistoryChangeRepository;
import com.proseg.msvc_maintenance.repository.TicketPhotoRepository;
import com.proseg.msvc_maintenance.repository.TicketRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import com.proseg.msvc_maintenance.security.Privileges;
import com.proseg.msvc_maintenance.websocket.TicketWebSocketEventDto;
import com.proseg.msvc_maintenance.websocket.TicketWebSocketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplStatusTest {

    private static final String CREATOR_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_USER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String ADMIN_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketAssetRepository ticketAssetRepository;
    @Mock
    private TicketPhotoRepository ticketPhotoRepository;
    @Mock
    private TicketCommentRepository ticketCommentRepository;
    @Mock
    private TicketHistoryChangeRepository ticketHistoryChangeRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TicketWebSocketManager webSocketManager;
    @Mock
    private AuthClient authClient;
    @Mock
    private MaintenanceNotificationProperties notificationProperties;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TicketServiceImpl service;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;
    @Captor
    private ArgumentCaptor<TicketHistoryChange> historyCaptor;
    @Captor
    private ArgumentCaptor<TicketWebSocketEventDto> webSocketCaptor;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private UUID ticketId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ticketOverdueHours", 72L);
        ReflectionTestUtils.setField(service, "archiveBaseUrl", "http://localhost:8081");
        lenient().when(notificationProperties.getExtraEmails()).thenReturn(List.of());
        lenient().when(authClient.findUsersByKeycloakIds(anyList()))
                .thenReturn(new ApiResponse<>("ok", List.of(), 200));

        ticketId = UUID.randomUUID();
        ticket = baseTicket(TicketStatus.OPEN, TicketPriority.MEDIUM);
    }

    @Test
    @DisplayName("updateStatus con ticket inexistente lanza notFound")
    void updateStatus_cuandoTicketNoExiste_lanzaNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.IN_PROGRESS).build(),
                jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_NOT_FOUND"));
    }

    @Test
    @DisplayName("updateStatus sin permisos y no creador lanza accessDenied")
    void updateStatus_cuandoSinPermisoNiCreador_lanzaAccessDenied() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.IN_PROGRESS).build(),
                jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_ACCESS_DENIED"));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus permite al creador del ticket")
    void updateStatus_cuandoEsCreador_actualizaEstado() {
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.IN_PROGRESS).build(),
                jwtAuth(CREATOR_ID));

        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("updateStatus permite admin y LEER_TODOS aunque no sean creadores")
    void updateStatus_cuandoAdminOLeerTodos_actualizaEstado() {
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.RESOLVED).build(),
                jwtAuth(ADMIN_ID, "admin"));

        verify(ticketRepository).save(any(Ticket.class));

        service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.CANCELLED).build(),
                jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER_TODOS));
    }

    @Test
    @DisplayName("updateStatus persiste historial, websocket y notificación cuando cambia el estado")
    void updateStatus_cuandoCambiaEstado_guardaHistorialEmiteEventos() {
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.IN_PROGRESS).build(),
                jwtAuth(CREATOR_ID));

        verify(ticketHistoryChangeRepository).save(historyCaptor.capture());
        TicketHistoryChange history = historyCaptor.getValue();
        assertThat(history.getType()).isEqualTo(TicketHistoryChangeType.STATUS_CHANGED);
        assertThat(history.getFieldName()).isEqualTo("status");
        assertThat(history.getOldValue()).isEqualTo("OPEN");
        assertThat(history.getNewValue()).isEqualTo("IN_PROGRESS");
        assertThat(history.getAuthorId()).isEqualTo(CREATOR_ID);

        verify(webSocketManager).broadcast(webSocketCaptor.capture());
        assertThat(webSocketCaptor.getValue().getType()).isEqualTo("ticket.status.updated");
        assertThat(webSocketCaptor.getValue().getTicketId()).isEqualTo(ticketId);
        assertThat(webSocketCaptor.getValue().getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TicketNotificationDomainEvent notification = (TicketNotificationDomainEvent) eventCaptor.getValue();
        assertThat(notification.actionType()).isEqualTo("STATUS_CHANGED");
        assertThat(notification.ticketId()).isEqualTo(ticketId);
        assertThat(notification.actorId()).isEqualTo(CREATOR_ID);
    }

    @Test
    @DisplayName("updateStatus con mismo estado no guarda historial pero sí websocket y notificación (sin validación de transición)")
    void updateStatus_cuandoEstadoIgual_noHistorialPeroSiSideEffects() {
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updateStatus(
                ticketId,
                TicketStatusUpdateRequestDto.builder().status(TicketStatus.OPEN).build(),
                jwtAuth(CREATOR_ID));

        verify(ticketHistoryChangeRepository, never()).save(any());
        verify(webSocketManager).broadcast(webSocketCaptor.capture());
        assertThat(webSocketCaptor.getValue().getType()).isEqualTo("ticket.status.updated");
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(((TicketNotificationDomainEvent) eventCaptor.getValue()).actionType()).isEqualTo("STATUS_CHANGED");
    }

    @Test
    @DisplayName("updatePriority sin permiso lanza priorityForbidden")
    void updatePriority_cuandoSinPermiso_lanzaPriorityForbidden() {
        assertThatThrownBy(() -> service.updatePriority(
                ticketId,
                TicketPriorityUpdateRequestDto.builder().priority(TicketPriority.HIGH).build(),
                jwtAuth(CREATOR_ID, Privileges.Tickets.LEER)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_PRIORITY_FORBIDDEN"));
    }

    @Test
    @DisplayName("updatePriority con admin o ASIGNAR_PRIORIDAD actualiza y emite eventos")
    void updatePriority_cuandoTienePermiso_guardaHistorialYEventos() {
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updatePriority(
                ticketId,
                TicketPriorityUpdateRequestDto.builder().priority(TicketPriority.HIGH).build(),
                jwtAuth(OTHER_USER_ID, Privileges.Tickets.ASIGNAR_PRIORIDAD));

        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getPriority()).isEqualTo(TicketPriority.HIGH);
        verify(ticketHistoryChangeRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getType()).isEqualTo(TicketHistoryChangeType.PRIORITY_CHANGED);
        verify(webSocketManager).broadcast(webSocketCaptor.capture());
        assertThat(webSocketCaptor.getValue().getType()).isEqualTo("ticket.priority.updated");
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(((TicketNotificationDomainEvent) eventCaptor.getValue()).actionType()).isEqualTo("PRIORITY_CHANGED");
    }

    @Test
    @DisplayName("updatePriority con ticket inexistente lanza notFound")
    void updatePriority_cuandoTicketNoExiste_lanzaNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePriority(
                ticketId,
                TicketPriorityUpdateRequestDto.builder().priority(TicketPriority.HIGH).build(),
                jwtAuth(ADMIN_ID, "admin")))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_NOT_FOUND"));
    }

    @Test
    @DisplayName("updatePriority con misma prioridad no guarda historial")
    void updatePriority_cuandoPrioridadIgual_noGuardaHistorial() {
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updatePriority(
                ticketId,
                TicketPriorityUpdateRequestDto.builder().priority(TicketPriority.MEDIUM).build(),
                jwtAuth(ADMIN_ID, "admin"));

        verify(ticketHistoryChangeRepository, never()).save(any());
        verify(webSocketManager).broadcast(any());
    }

    @Test
    @DisplayName("updateAssignedTo sin permiso lanza assignForbidden")
    void updateAssignedTo_cuandoSinPermiso_lanzaAssignForbidden() {
        assertThatThrownBy(() -> service.updateAssignedTo(
                ticketId,
                TicketAssignedToUpdateRequestDto.builder().assignedTo(UUID.randomUUID()).build(),
                jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_ASSIGN_FORBIDDEN"));
    }

    @Test
    @DisplayName("updateAssignedTo no valida que el usuario exista en Keycloak")
    void updateAssignedTo_cuandoUuidArbitrario_persisteSinValidarUsuario() {
        UUID assignee = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        stubSaveReturnsInput();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.updateAssignedTo(
                ticketId,
                TicketAssignedToUpdateRequestDto.builder().assignedTo(assignee).build(),
                jwtAuth(ADMIN_ID, "admin"));

        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getAssignedTo()).isEqualTo(assignee);
        verify(ticketHistoryChangeRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFieldName()).isEqualTo("assignedTo");
        verify(webSocketManager).broadcast(webSocketCaptor.capture());
        assertThat(webSocketCaptor.getValue().getType()).isEqualTo("ticket.assignedTo.updated");
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(((TicketNotificationDomainEvent) eventCaptor.getValue()).actionType()).isEqualTo("ASSIGNED_TO_CHANGED");
    }

    @Test
    @DisplayName("findAssignableUsers sin permiso lanza assignableUsersForbidden")
    void findAssignableUsers_cuandoSinPermiso_lanzaForbidden() {
        assertThatThrownBy(() -> service.findAssignableUsers(jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_ASSIGNABLE_USERS_FORBIDDEN"));
    }

    @Test
    @DisplayName("findAssignableUsers con ASIGNAR_TICKET consulta auth y deduplica por id")
    void findAssignableUsers_cuandoTienePermiso_retornaUsuariosUnicos() {
        KeycloakUserResponse u1 = new KeycloakUserResponse("id-1", "ana", "a@x.com", "Ana", "Perez", "ACTIVE", List.of());
        KeycloakUserResponse u1Dup = new KeycloakUserResponse("id-1", "ana2", "a2@x.com", "Ana", "P", "ACTIVE", List.of());
        KeycloakUserResponse u2 = new KeycloakUserResponse("id-2", "bob", "b@x.com", "Bob", "L", "ACTIVE", List.of());
        PagedResponse<KeycloakUserResponse> page = PagedResponse.<KeycloakUserResponse>builder()
                .content(List.of(u1, u1Dup, u2))
                .totalPages(1)
                .build();
        when(authClient.findUsers(eq(0), eq(200))).thenReturn(new ApiResponse<>("ok", page, 200));

        List<KeycloakUserResponse> users = service.findAssignableUsers(
                jwtAuth(OTHER_USER_ID, Privileges.Tickets.ASIGNAR_TICKET));

        assertThat(users).hasSize(2);
        assertThat(users).extracting(KeycloakUserResponse::id).containsExactlyInAnyOrder("id-1", "id-2");
        verify(authClient).findUsers(0, 200);
    }

    private void stubSaveReturnsInput() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Ticket baseTicket(TicketStatus status, TicketPriority priority) {
        Ticket t = Ticket.builder()
                .id(ticketId)
                .title("Falla AC")
                .description("No enfría")
                .status(status)
                .priority(priority)
                .build();
        t.setCreatedBy(CREATOR_ID);
        ReflectionTestUtils.setField(t, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(t, "updatedAt", LocalDateTime.now());
        return t;
    }

    private static JwtAuthenticationToken jwtAuth(String subject, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();
        var granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new JwtAuthenticationToken(jwt, granted);
    }
}
