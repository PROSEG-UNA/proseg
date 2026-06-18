package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.TicketAssignedRoleUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCommentCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.sssi.msvc_maintenance.dto.request.TicketPriorityUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.TicketCommentResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketListResponseDto;
import com.sssi.msvc_maintenance.dto.response.TicketResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    TicketResponseDto create(TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication);
    TicketResponseDto update(UUID id, TicketCreateRequestDto request, List<MultipartFile> photos, Authentication authentication);
    Page<TicketListResponseDto> findAll(Pageable pageable, Authentication authentication);
    TicketResponseDto findById(UUID id, Authentication authentication);
    TicketResponseDto updatePriority(UUID id, TicketPriorityUpdateRequestDto request, Authentication authentication);
    TicketResponseDto updateAssignedRole(UUID id, TicketAssignedRoleUpdateRequestDto request, Authentication authentication);
    TicketResponseDto resolve(UUID id, Authentication authentication);
    TicketCommentResponseDto addComment(UUID id, TicketCommentCreateRequestDto request, Authentication authentication);

    Page<com.sssi.msvc_maintenance.dto.response.TicketHistoryChangeResponseDto> findHistoryByTicket(UUID id, Pageable pageable, Authentication authentication);
}