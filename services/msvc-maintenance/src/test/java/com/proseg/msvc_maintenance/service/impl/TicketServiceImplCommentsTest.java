package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.proseg.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketCommentUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.response.TicketCommentResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketPhotoResponseDto;
import com.proseg.msvc_maintenance.entity.Ticket;
import com.proseg.msvc_maintenance.entity.TicketComment;
import com.proseg.msvc_maintenance.entity.TicketHistoryChange;
import com.proseg.msvc_maintenance.entity.TicketPhoto;
import com.proseg.msvc_maintenance.entity.enums.TicketHistoryChangeType;
import com.proseg.msvc_maintenance.event.TicketNotificationDomainEvent;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.ADMIN_ID;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.CREATOR_ID;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.OTHER_USER_ID;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.baseTicket;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.initService;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.jwtAuth;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.keycloakUser;
import static com.proseg.msvc_maintenance.service.impl.TicketServiceImplTestSupport.stubAuthorNames;
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
class TicketServiceImplCommentsTest {

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
    private ArgumentCaptor<TicketComment> commentCaptor;
    @Captor
    private ArgumentCaptor<TicketHistoryChange> historyCaptor;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private UUID ticketId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        initService(service);
        lenient().when(notificationProperties.getExtraEmails()).thenReturn(List.of());
        stubAuthorNames(authClient);

        ticketId = UUID.randomUUID();
        ticket = baseTicket(ticketId);
        ticket.setTicketComments(new HashSet<>());
        ticket.setTicketPhotos(new HashSet<>());
    }

    @Test
    @DisplayName("addComment con ticket inexistente lanza notFound")
    void addComment_cuandoTicketNoExiste_lanzaNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addComment(
                ticketId,
                TicketCommentCreateRequestDto.builder().content("hola").build(),
                jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_NOT_FOUND"));
    }

    @Test
    @DisplayName("addComment sin permiso ni creador lanza accessDenied")
    void addComment_cuandoSinAcceso_lanzaAccessDenied() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.addComment(
                ticketId,
                TicketCommentCreateRequestDto.builder().content("hola").build(),
                jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_ACCESS_DENIED"));

        verify(ticketCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("addComment feliz recorta contenido, guarda historial COMMENT_ADDED y notifica con nombre del autor")
    void addComment_cuandoAutorizado_persisteHistorialYNotificacion() {
        stubAuthorNames(authClient, keycloakUser(CREATOR_ID, "Ana", "Perez"));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(inv -> {
            TicketComment saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        TicketCommentResponseDto response = service.addComment(
                ticketId,
                TicketCommentCreateRequestDto.builder().content("  observación  ").build(),
                jwtAuth(CREATOR_ID));

        verify(ticketCommentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getContent()).isEqualTo("observación");
        assertThat(commentCaptor.getValue().getAuthorId()).isEqualTo(CREATOR_ID);
        assertThat(response.getContent()).isEqualTo("observación");
        assertThat(response.getAuthorName()).isEqualTo("Ana Perez");

        verify(ticketHistoryChangeRepository).save(historyCaptor.capture());
        TicketHistoryChange history = historyCaptor.getValue();
        assertThat(history.getType()).isEqualTo(TicketHistoryChangeType.COMMENT_ADDED);
        assertThat(history.getNewValue()).isEqualTo("observación");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TicketNotificationDomainEvent notification = (TicketNotificationDomainEvent) eventCaptor.getValue();
        assertThat(notification.actionType()).isEqualTo("COMMENT_ADDED");
        assertThat(notification.changedFields()).containsExactly("Comentario de Ana Perez: observación");
    }

    @Test
    @DisplayName("updateComment con comentario inexistente lanza commentNotFound")
    void updateComment_cuandoComentarioNoExiste_lanzaCommentNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateComment(
                ticketId,
                UUID.randomUUID(),
                TicketCommentUpdateRequestDto.builder().content("nuevo").build(),
                jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("updateComment solo permite editar al autor del comentario (admin no sustituye autoría)")
    void updateComment_cuandoAdminNoEsAutor_lanzaCommentEditForbidden() {
        TicketComment comment = commentOnTicket(CREATOR_ID, "texto");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateComment(
                ticketId,
                comment.getId(),
                TicketCommentUpdateRequestDto.builder().content("editado").build(),
                jwtAuth(ADMIN_ID, "admin")))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_COMMENT_EDIT_FORBIDDEN"));
    }

    @Test
    @DisplayName("updateComment feliz del autor guarda historial COMMENT_EDITED")
    void updateComment_cuandoEsAutor_guardaHistorialYNotifica() {
        stubAuthorNames(authClient, keycloakUser(CREATOR_ID, "Ana", "Perez"));
        TicketComment comment = commentOnTicket(CREATOR_ID, "viejo");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateComment(
                ticketId,
                comment.getId(),
                TicketCommentUpdateRequestDto.builder().content("  nuevo  ").build(),
                jwtAuth(CREATOR_ID));

        verify(ticketHistoryChangeRepository).save(historyCaptor.capture());
        TicketHistoryChange history = historyCaptor.getValue();
        assertThat(history.getType()).isEqualTo(TicketHistoryChangeType.COMMENT_EDITED);
        assertThat(history.getOldValue()).isEqualTo("viejo");
        assertThat(history.getNewValue()).isEqualTo("nuevo");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(((TicketNotificationDomainEvent) eventCaptor.getValue()).actionType()).isEqualTo("COMMENT_EDITED");
    }

    @Test
    @DisplayName("deleteComment con comentario inexistente lanza commentNotFound")
    void deleteComment_cuandoComentarioNoExiste_lanzaCommentNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.deleteComment(ticketId, UUID.randomUUID(), jwtAuth(CREATOR_ID)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("deleteComment solo permite borrar al autor (admin con acceso al ticket no basta)")
    void deleteComment_cuandoAdminNoEsAutor_lanzaCommentDeleteForbidden() {
        TicketComment comment = commentOnTicket(CREATOR_ID, "borrar");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.deleteComment(ticketId, comment.getId(), jwtAuth(ADMIN_ID, "admin")))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_COMMENT_DELETE_FORBIDDEN"));

        verify(ticketCommentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteComment del autor elimina comentario y registra COMMENT_REMOVED")
    void deleteComment_cuandoEsAutor_eliminaYRegistraHistorial() {
        stubAuthorNames(authClient, keycloakUser(CREATOR_ID, "Ana", "Perez"));
        TicketComment comment = commentOnTicket(CREATOR_ID, "contenido");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        service.deleteComment(ticketId, comment.getId(), jwtAuth(CREATOR_ID));

        verify(ticketCommentRepository).delete(comment);
        verify(ticketHistoryChangeRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getType()).isEqualTo(TicketHistoryChangeType.COMMENT_REMOVED);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(((TicketNotificationDomainEvent) eventCaptor.getValue()).actionType()).isEqualTo("COMMENT_REMOVED");
    }

    @Test
    @DisplayName("findHistoryByTicket sin acceso lanza accessDenied")
    void findHistoryByTicket_cuandoSinPermiso_lanzaAccessDenied() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.findHistoryByTicket(
                ticketId, PageRequest.of(0, 10), jwtAuth(OTHER_USER_ID, Privileges.Tickets.LEER)))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> assertThat(((TicketException) ex).getErrorCode()).isEqualTo("TICKET_ACCESS_DENIED"));

        verify(ticketHistoryChangeRepository, never()).findByTicketIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("findHistoryByTicket feliz delega paginación al repositorio")
    void findHistoryByTicket_cuandoTieneAcceso_retornaPagina() {
        UUID historyId = UUID.randomUUID();
        TicketHistoryChange change = TicketHistoryChange.builder()
                .id(historyId)
                .ticket(ticket)
                .type(TicketHistoryChangeType.STATUS_CHANGED)
                .fieldName("status")
                .authorId(CREATOR_ID)
                .build();
        Pageable request = PageRequest.of(1, 5);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketHistoryChangeRepository.findByTicketIdOrderByCreatedAtDesc(eq(ticketId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(change)));

        Page<TicketHistoryChangeResponseDto> page = service.findHistoryByTicket(
                ticketId, request, jwtAuth(CREATOR_ID));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(historyId);
        verify(ticketHistoryChangeRepository).findByTicketIdOrderByCreatedAtDesc(eq(ticketId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("getPhotos sin fotos devuelve lista vacía")
    void getPhotos_cuandoSinAdjuntos_retornaListaVacia() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        List<TicketPhotoResponseDto> photos = service.getPhotos(ticketId, jwtAuth(CREATOR_ID));

        assertThat(photos).isEmpty();
    }

    @Test
    @DisplayName("getPhotos con adjuntos construye URL presignada local")
    void getPhotos_cuandoHayAdjuntos_retornaMetadatosYUrl() {
        TicketPhoto photo = TicketPhoto.builder()
                .id(UUID.randomUUID())
                .objectName("folder/photo.jpg")
                .fileName("photo.jpg")
                .contentType("image/jpeg")
                .ticket(ticket)
                .build();
        ticket.getTicketPhotos().add(photo);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        List<TicketPhotoResponseDto> photos = service.getPhotos(ticketId, jwtAuth(CREATOR_ID));

        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getObjectName()).isEqualTo("folder/photo.jpg");
        assertThat(photos.get(0).getImageUrl()).isEqualTo("/api/v1/archive/files/folder/photo.jpg");
    }

    @Test
    @DisplayName("getPhotos con objectName vacío deja imageUrl null (no lanza relatedPhotoUnavailable)")
    void getPhotos_cuandoObjectNameInvalido_noLanzaExcepcion() {
        TicketPhoto photo = TicketPhoto.builder()
                .id(UUID.randomUUID())
                .objectName("   ")
                .fileName("photo.jpg")
                .contentType("image/jpeg")
                .ticket(ticket)
                .build();
        ticket.getTicketPhotos().add(photo);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        List<TicketPhotoResponseDto> photos = service.getPhotos(ticketId, jwtAuth(CREATOR_ID));

        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getImageUrl()).isNull();
    }

    private TicketComment commentOnTicket(String authorId, String content) {
        TicketComment comment = TicketComment.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .authorId(authorId)
                .content(content)
                .build();
        ticket.getTicketComments().add(comment);
        return comment;
    }
}
