package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvc_maintenance.dto.request.TicketAssignedToUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketStatusUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.TicketCommentResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.sssi.msvc_maintenance.dto.response.TicketListResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketPhotoResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketResponseDto;
import com.sssi.msvc_maintenance.entity.enums.TicketStatus;
import com.sssi.msvc_maintenance.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.tickets:/api/v1/maintenance/tickets}")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TicketResponseDto>> create(
            @RequestPart("ticket") @Valid TicketCreateRequestDto request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            Authentication authentication) {
        return ApiResponseBuilder.created(
                ticketService.create(request, photos, authentication),
                "Ticket creado correctamente"
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TicketResponseDto>> update(
            @PathVariable UUID id,
            @RequestPart("ticket") @Valid TicketCreateRequestDto request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                ticketService.update(id, request, photos, authentication),
                "Ticket actualizado correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketListResponseDto>>> findAll(
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                PageMapper.from(ticketService.findAll(status, pageable, authentication)),
                "Lista de tickets"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponseDto>> findById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.findById(id, authentication), "Ticket obtenido correctamente");
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<PageResponse<com.sssi.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto>>> history(
            @PathVariable UUID id,
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                PageMapper.from(ticketService.findHistoryByTicket(id, pageable, authentication)),
                "Historial de cambios"
        );
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<ApiResponse<TicketResponseDto>> updatePriority(
            @PathVariable UUID id,
            @Valid @RequestBody TicketPriorityUpdateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.updatePriority(id, request, authentication), "Prioridad actualizada correctamente");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TicketStatusUpdateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.updateStatus(id, request, authentication), "Estado actualizado correctamente");
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<ApiResponse<java.util.List<TicketPhotoResponseDto>>> getPhotos(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.getPhotos(id, authentication), "Fotos obtenidas correctamente");
    }

    @GetMapping("/assignees")
    public ResponseEntity<ApiResponse<List<KeycloakUserResponse>>> getAssignees(
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.findAssignableUsers(authentication), "Usuarios obtenidos correctamente");
    }

    @PatchMapping("/{id}/assigned-to")
    public ResponseEntity<ApiResponse<TicketResponseDto>> updateAssignedTo(
            @PathVariable UUID id,
            @Valid @RequestBody TicketAssignedToUpdateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.updateAssignedTo(id, request, authentication), "Ticket asignado correctamente");
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<TicketResponseDto>> resolve(
            @PathVariable UUID id,
            Authentication authentication) {
        return ApiResponseBuilder.ok(ticketService.resolve(id, authentication), "Ticket resuelto correctamente");
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TicketCommentResponseDto>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody TicketCommentCreateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                ticketService.addComment(id, request, authentication),
                "Comentario agregado correctamente"
        );
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<TicketCommentResponseDto>> updateComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId,
            @Valid @RequestBody TicketCommentUpdateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                ticketService.updateComment(id, commentId, request, authentication),
                "Comentario actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId,
            Authentication authentication) {
        ticketService.deleteComment(id, commentId, authentication);
        return ApiResponseBuilder.ok(null, "Comentario eliminado correctamente");
    }
}