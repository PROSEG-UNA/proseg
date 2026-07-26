package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.TicketAssignedToUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketStatusUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.TicketCommentResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketDashboardSummaryResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.sssi.msvc_maintenance.dto.response.TicketListResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketPhotoResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketResponseDto;
import com.sssi.msvc_maintenance.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    TicketResponseDto create(TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication);
    TicketResponseDto update(UUID id, TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication);
    Page<TicketListResponseDto> findAll(TicketStatus status, Pageable pageable, Authentication authentication);
    TicketResponseDto findById(UUID id, Authentication authentication);
    TicketResponseDto updatePriority(UUID id, TicketPriorityUpdateRequestDto request, Authentication authentication);
    List<KeycloakUserResponse> findAssignableUsers(Authentication authentication);
    TicketResponseDto updateAssignedTo(UUID id, TicketAssignedToUpdateRequestDto request, Authentication authentication);
    TicketResponseDto updateStatus(UUID id, TicketStatusUpdateRequestDto request, Authentication authentication);
    TicketDashboardSummaryResponseDto getDashboardSummary(Authentication authentication);
    List<TicketPhotoResponseDto> getPhotos(UUID id, Authentication authentication);
    TicketCommentResponseDto addComment(UUID id, TicketCommentCreateRequestDto request, Authentication authentication);
    TicketCommentResponseDto updateComment(UUID ticketId, UUID commentId, TicketCommentUpdateRequestDto request, Authentication authentication);
    void deleteComment(UUID ticketId, UUID commentId, Authentication authentication);
    Page<com.sssi.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto> findHistoryByTicket(UUID id, Pageable pageable, Authentication authentication);
}