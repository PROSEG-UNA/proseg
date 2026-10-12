package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.proseg.msvc_maintenance.dto.response.TicketDashboardSummaryResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketListResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketResponseDto;
import com.proseg.msvc_maintenance.entity.Ticket;
import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import com.proseg.msvc_maintenance.exception.TicketException;
import com.proseg.msvc_maintenance.repository.TicketAssetRepository;
import com.proseg.msvc_maintenance.repository.TicketCommentRepository;
import com.proseg.msvc_maintenance.repository.TicketHistoryChangeRepository;
import com.proseg.msvc_maintenance.repository.TicketPhotoRepository;
import com.proseg.msvc_maintenance.repository.TicketRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import com.proseg.msvc_maintenance.security.Privileges;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
class TicketServiceImplQueryTest {

    private static final String CREATOR_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_USER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

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
    private ArgumentCaptor<Pageable> pageableCaptor;

    private UUID ticketId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ticketOverdueHours", 72L);
        ReflectionTestUtils.setField(service, "archiveBaseUrl", "http://localhost:8081");
        lenient().when(authClient.findUsersByKeycloakIds(anyList()))
                .thenReturn(new ApiResponse<>("ok", List.of(), 200));
        lenient().when(userCompanyRepository.findAllByKeycloakUserId(any())).thenReturn(List.of());

        ticketId = UUID.randomUUID();
        ticket = Ticket.builder()
                .id(ticketId)
                .title("Falla AC")
                .description("No enfría")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .build();
        ticket.setCreatedBy(CREATOR_ID);
        ReflectionTestUtils.setField(ticket, "createdAt", LocalDateTime.now().minusDays(2));
        ReflectionTestUtils.setField(ticket, "updatedAt", LocalDateTime.now());
    }

    @Test
    @DisplayName("findById con ticket inexistente lanza notFound")
    void findById_cuandoNoExiste_lanzaNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ticketId, jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_NOT_FOUND"));
    }

    @Test
    @DisplayName("findById sin permiso ni creador lanza accessDenied")
    void findById_cuandoSinAcceso_lanzaAccessDenied() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.findById(ticketId, jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("findById permite al creador y a LEER_TODOS")
    void findById_cuandoCreadorOLeerTodos_retornaDto() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        TicketResponseDto byCreator = service.findById(ticketId, jwtAuth(CREATOR_ID));
        assertThat(byCreator.getId()).isEqualTo(ticketId);

        TicketResponseDto byViewer = service.findById(ticketId, jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER_TODOS));
        assertThat(byViewer.getTitle()).isEqualTo("Falla AC");
    }

    @Test
    @DisplayName("findAll con LEER_TODOS consulta todos los tickets por estado")
    void findAll_cuandoLeerTodos_usaRepositorioGlobal() {
        Pageable request = PageRequest.of(1, 10);
        when(ticketRepository.findByStatus(eq(TicketStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        Page<TicketListResponseDto> page = service.findAll(
                TicketStatus.OPEN, request, jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER_TODOS));

        assertThat(page.getContent()).hasSize(1);
        verify(ticketRepository).findByStatus(eq(TicketStatus.OPEN), pageableCaptor.capture());
        assertSortedByCreatedAtDesc(pageableCaptor.getValue());
        verify(ticketRepository, never()).findByCreatedBy(any(), any());
    }

    @Test
    @DisplayName("findAll sin permiso global filtra por creador")
    void findAll_cuandoUsuarioNormal_filtraPorCreador() {
        Pageable request = PageRequest.of(0, 5);
        when(ticketRepository.findByCreatedByAndStatus(eq(CREATOR_ID), eq(TicketStatus.IN_PROGRESS), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        service.findAll(TicketStatus.IN_PROGRESS, request, jwtAuth(CREATOR_ID));

        verify(ticketRepository).findByCreatedByAndStatus(eq(CREATOR_ID), eq(TicketStatus.IN_PROGRESS), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertSortedByCreatedAtDesc(pageableCaptor.getValue());
        verify(ticketRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("findAll sin estado usa findAll o findByCreatedBy según permiso")
    void findAll_cuandoSinEstado_eligeConsultaPorPermiso() {
        Pageable request = PageRequest.of(0, 20);
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(ticket)));
        when(ticketRepository.findByCreatedBy(eq(CREATOR_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        service.findAll(null, request, jwtAuth(OTHER_USER_ID, "admin"));
        verify(ticketRepository).findAll(pageableCaptor.capture());
        assertSortedByCreatedAtDesc(pageableCaptor.getValue());

        service.findAll(null, request, jwtAuth(CREATOR_ID));
        verify(ticketRepository).findByCreatedBy(eq(CREATOR_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("getDashboardSummary con admin agrega todos los tickets del repositorio")
    void getDashboardSummary_cuandoAdmin_usaFindAll() {
        Ticket resolved = Ticket.builder()
                .title("T2")
                .description("d")
                .status(TicketStatus.RESOLVED)
                .priority(TicketPriority.LOW)
                .build();
        resolved.setCreatedBy(CREATOR_ID);
        ReflectionTestUtils.setField(resolved, "createdAt", LocalDateTime.now().minusDays(10));
        ReflectionTestUtils.setField(resolved, "updatedAt", LocalDateTime.now().minusDays(9));
        when(ticketRepository.findAll()).thenReturn(List.of(ticket, resolved));

        TicketDashboardSummaryResponseDto summary = service.getDashboardSummary(jwtAuth(OTHER_USER_ID, "admin"));

        assertThat(summary.getTotalTickets()).isEqualTo(2);
        assertThat(summary.getPendingTickets()).isEqualTo(1);
        assertThat(summary.getResolvedTickets()).isEqualTo(1);
        verify(ticketRepository).findAll();
        verify(ticketRepository, never()).findByCreatedBy(any(), any());
    }

    @Test
    @DisplayName("getDashboardSummary sin permiso global usa solo tickets del creador")
    void getDashboardSummary_cuandoUsuarioNormal_filtraPorCreador() {
        when(ticketRepository.findByCreatedBy(eq(CREATOR_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        TicketDashboardSummaryResponseDto summary = service.getDashboardSummary(jwtAuth(CREATOR_ID));

        assertThat(summary.getTotalTickets()).isEqualTo(1);
        assertThat(summary.getPendingTickets()).isEqualTo(1);
        verify(ticketRepository).findByCreatedBy(eq(CREATOR_ID), eq(Pageable.unpaged()));
        verify(ticketRepository, never()).findAll();
    }

    private static void assertSortedByCreatedAtDesc(Pageable pageable) {
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
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
