package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.dto.request.TicketAssignedToUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketCommentUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.request.TicketStatusUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.proseg.msvc_maintenance.dto.response.TicketCommentResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketDashboardSummaryResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketListResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketPhotoResponseDto;
import com.proseg.msvc_maintenance.dto.response.TicketResponseDto;
import com.proseg.msvc_maintenance.entity.enums.TicketPriority;
import com.proseg.msvc_maintenance.entity.enums.TicketStatus;
import com.proseg.msvc_maintenance.exception.TicketException;
import com.proseg.msvc_maintenance.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    private MockMvc mockMvc;
    private UUID ticketId;
    private UUID commentId;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new TicketController(ticketService),
                "routes.tickets",
                ControllerMockMvcSupport.TICKETS);
        ticketId = UUID.randomUUID();
        commentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET listado devuelve tickets paginados")
    void findAll_cuandoHayDatos_retornaOk() throws Exception {
        TicketListResponseDto dto = TicketListResponseDto.builder().id(ticketId).build();
        when(ticketService.findAll(eq(null), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.TICKETS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(ticketId.toString()));
    }

    @Test
    @DisplayName("GET listado propaga acceso denegado")
    void findAll_cuandoSinPermiso_retornaForbidden() throws Exception {
        when(ticketService.findAll(any(), any(), any())).thenThrow(TicketException.accessDenied());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.TICKETS)),
                403,
                "TICKET_ACCESS_DENIED");
    }

    @Test
    @DisplayName("GET by id devuelve ticket")
    void findById_cuandoExiste_retornaOk() throws Exception {
        TicketResponseDto dto = TicketResponseDto.builder().id(ticketId).build();
        when(ticketService.findById(eq(ticketId), any())).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId.toString()));
    }

    @Test
    @DisplayName("GET by id propaga not found")
    void findById_cuandoNoExiste_retornaNotFound() throws Exception {
        when(ticketService.findById(eq(ticketId), any())).thenThrow(TicketException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/" + ticketId)),
                404,
                "TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("GET history devuelve cambios paginados")
    void history_cuandoHayDatos_retornaOk() throws Exception {
        TicketHistoryChangeResponseDto change = TicketHistoryChangeResponseDto.builder()
                .id(UUID.randomUUID())
                .build();
        when(ticketService.findHistoryByTicket(eq(ticketId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(change), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").exists());
    }

    @Test
    @DisplayName("GET history propaga not found")
    void history_cuandoTicketNoExiste_retornaNotFound() throws Exception {
        when(ticketService.findHistoryByTicket(eq(ticketId), any(), any())).thenThrow(TicketException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/history")),
                404,
                "TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("PATCH priority actualiza ticket")
    void updatePriority_cuandoPermitido_retornaOk() throws Exception {
        TicketPriorityUpdateRequestDto body = TicketPriorityUpdateRequestDto.builder()
                .priority(TicketPriority.HIGH)
                .build();
        TicketResponseDto dto = TicketResponseDto.builder().id(ticketId).build();
        when(ticketService.updatePriority(eq(ticketId), any(), any())).thenReturn(dto);

        mockMvc.perform(patch(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId.toString()));
    }

    @Test
    @DisplayName("PATCH priority propaga forbidden")
    void updatePriority_cuandoSinPermiso_retornaForbidden() throws Exception {
        TicketPriorityUpdateRequestDto body = TicketPriorityUpdateRequestDto.builder()
                .priority(TicketPriority.LOW)
                .build();
        when(ticketService.updatePriority(eq(ticketId), any(), any())).thenThrow(TicketException.priorityForbidden());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(patch(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                403,
                "TICKET_PRIORITY_FORBIDDEN");
    }

    @Test
    @DisplayName("PATCH status actualiza ticket")
    void updateStatus_cuandoPermitido_retornaOk() throws Exception {
        TicketStatusUpdateRequestDto body = TicketStatusUpdateRequestDto.builder()
                .status(TicketStatus.IN_PROGRESS)
                .build();
        TicketResponseDto dto = TicketResponseDto.builder().id(ticketId).build();
        when(ticketService.updateStatus(eq(ticketId), any(), any())).thenReturn(dto);

        mockMvc.perform(patch(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId.toString()));
    }

    @Test
    @DisplayName("PATCH status propaga access denied")
    void updateStatus_cuandoSinPermiso_retornaForbidden() throws Exception {
        TicketStatusUpdateRequestDto body = TicketStatusUpdateRequestDto.builder()
                .status(TicketStatus.RESOLVED)
                .build();
        when(ticketService.updateStatus(eq(ticketId), any(), any())).thenThrow(TicketException.accessDenied());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(patch(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                403,
                "TICKET_ACCESS_DENIED");
    }

    @Test
    @DisplayName("GET dashboard devuelve resumen")
    void getDashboardSummary_cuandoOk_retornaOk() throws Exception {
        TicketDashboardSummaryResponseDto summary = TicketDashboardSummaryResponseDto.builder()
                .totalTickets(3L)
                .build();
        when(ticketService.getDashboardSummary(any())).thenReturn(summary);

        mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTickets").value(3));
    }

    @Test
    @DisplayName("GET dashboard propaga error")
    void getDashboardSummary_cuandoFalla_retornaForbidden() throws Exception {
        when(ticketService.getDashboardSummary(any())).thenThrow(TicketException.accessDenied());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/dashboard")),
                403,
                "TICKET_ACCESS_DENIED");
    }

    @Test
    @DisplayName("GET photos devuelve lista")
    void getPhotos_cuandoHayFotos_retornaOk() throws Exception {
        TicketPhotoResponseDto photo = TicketPhotoResponseDto.builder().objectName("img.png").build();
        when(ticketService.getPhotos(eq(ticketId), any())).thenReturn(List.of(photo));

        mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].objectName").value("img.png"));
    }

    @Test
    @DisplayName("GET photos propaga not found de ticket")
    void getPhotos_cuandoTicketNoExiste_retornaNotFound() throws Exception {
        when(ticketService.getPhotos(eq(ticketId), any())).thenThrow(TicketException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/photos")),
                404,
                "TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("GET assignees devuelve usuarios")
    void getAssignees_cuandoPermitido_retornaOk() throws Exception {
        KeycloakUserResponse user = new KeycloakUserResponse(
                "kc-1", "tech", "t@example.com", "T", "U", "ACTIVE", List.of());
        when(ticketService.findAssignableUsers(any())).thenReturn(List.of(user));

        mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/assignees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("kc-1"));
    }

    @Test
    @DisplayName("GET assignees propaga forbidden")
    void getAssignees_cuandoSinPermiso_retornaForbidden() throws Exception {
        when(ticketService.findAssignableUsers(any())).thenThrow(TicketException.assignableUsersForbidden());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.TICKETS + "/assignees")),
                403,
                "TICKET_ASSIGNABLE_USERS_FORBIDDEN");
    }

    @Test
    @DisplayName("PATCH assigned-to actualiza ticket")
    void updateAssignedTo_cuandoPermitido_retornaOk() throws Exception {
        UUID assignee = UUID.randomUUID();
        TicketAssignedToUpdateRequestDto body = TicketAssignedToUpdateRequestDto.builder()
                .assignedTo(assignee)
                .build();
        TicketResponseDto dto = TicketResponseDto.builder().id(ticketId).build();
        when(ticketService.updateAssignedTo(eq(ticketId), any(), any())).thenReturn(dto);

        mockMvc.perform(patch(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/assigned-to")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId.toString()));
    }

    @Test
    @DisplayName("PATCH assigned-to propaga forbidden")
    void updateAssignedTo_cuandoSinPermiso_retornaForbidden() throws Exception {
        TicketAssignedToUpdateRequestDto body = TicketAssignedToUpdateRequestDto.builder()
                .assignedTo(UUID.randomUUID())
                .build();
        when(ticketService.updateAssignedTo(eq(ticketId), any(), any())).thenThrow(TicketException.assignForbidden());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(patch(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/assigned-to")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                403,
                "TICKET_ASSIGN_FORBIDDEN");
    }

    @Test
    @DisplayName("POST comment captura contenido")
    void addComment_cuandoValido_retornaOk() throws Exception {
        TicketCommentCreateRequestDto body = TicketCommentCreateRequestDto.builder()
                .content("Comentario de prueba")
                .build();
        TicketCommentResponseDto dto = TicketCommentResponseDto.builder().id(commentId).content("Comentario de prueba").build();
        ArgumentCaptor<TicketCommentCreateRequestDto> captor = ArgumentCaptor.forClass(TicketCommentCreateRequestDto.class);
        when(ticketService.addComment(eq(ticketId), any(), any())).thenReturn(dto);

        mockMvc.perform(post(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Comentario de prueba"));

        verify(ticketService).addComment(eq(ticketId), captor.capture(), any());
        assertThat(captor.getValue().getContent()).isEqualTo("Comentario de prueba");
    }

    @Test
    @DisplayName("POST comment propaga access denied")
    void addComment_cuandoSinPermiso_retornaForbidden() throws Exception {
        TicketCommentCreateRequestDto body = TicketCommentCreateRequestDto.builder().content("Hola").build();
        when(ticketService.addComment(eq(ticketId), any(), any())).thenThrow(TicketException.accessDenied());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                403,
                "TICKET_ACCESS_DENIED");
    }

    @Test
    @DisplayName("PUT comment actualiza contenido")
    void updateComment_cuandoValido_retornaOk() throws Exception {
        TicketCommentUpdateRequestDto body = TicketCommentUpdateRequestDto.builder()
                .content("Editado")
                .build();
        TicketCommentResponseDto dto = TicketCommentResponseDto.builder().id(commentId).content("Editado").build();
        when(ticketService.updateComment(eq(ticketId), eq(commentId), any(), any())).thenReturn(dto);

        mockMvc.perform(put(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Editado"));
    }

    @Test
    @DisplayName("PUT comment propaga edit forbidden")
    void updateComment_cuandoNoEsAutor_retornaForbidden() throws Exception {
        TicketCommentUpdateRequestDto body = TicketCommentUpdateRequestDto.builder().content("Editado").build();
        when(ticketService.updateComment(eq(ticketId), eq(commentId), any(), any()))
                .thenThrow(TicketException.commentEditForbidden());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(put(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                403,
                "TICKET_COMMENT_EDIT_FORBIDDEN");
    }

    @Test
    @DisplayName("DELETE comment invoca servicio")
    void deleteComment_cuandoValido_retornaOk() throws Exception {
        mockMvc.perform(delete(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(ticketService).deleteComment(eq(ticketId), eq(commentId), any());
    }

    @Test
    @DisplayName("DELETE comment propaga delete forbidden")
    void deleteComment_cuandoNoEsAutor_retornaForbidden() throws Exception {
        org.mockito.Mockito.doThrow(TicketException.commentDeleteForbidden())
                .when(ticketService).deleteComment(eq(ticketId), eq(commentId), any());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(delete(ControllerMockMvcSupport.TICKETS + "/" + ticketId + "/comments/" + commentId)),
                403,
                "TICKET_COMMENT_DELETE_FORBIDDEN");
    }
}
