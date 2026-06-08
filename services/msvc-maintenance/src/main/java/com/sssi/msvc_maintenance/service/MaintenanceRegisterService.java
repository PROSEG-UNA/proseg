package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.MaintenanceRegisterUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRegisterResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface MaintenanceRegisterService {

    Page<MaintenanceRequestResponseDto> findAssigned(String keycloakUserId, MaintenanceStatus status, Pageable pageable);

    Page<MaintenanceRequestResponseDto> findHistory(String keycloakUserId, boolean companyWide, Pageable pageable);

    MaintenanceRegisterResponseDto getOrCreateByRequest(UUID requestId);

    MaintenanceRegisterResponseDto update(UUID id, MaintenanceRegisterUpdateRequestDto request);

    MaintenanceRegisterResponseDto finalizeRegister(UUID id);

    Page<MaintenanceAssetOptionDto> findRegisterAssets(UUID id, String search, Map<String, String> filters, Pageable pageable);
}
