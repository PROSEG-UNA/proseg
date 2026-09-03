package com.proseg.msvc_maintenance.service;

import com.proseg.msvc_maintenance.dto.request.MaintenanceRecordRequestDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MaintenanceRecordService {

    MaintenanceRecordResponseDto create(UUID registerId, MaintenanceRecordRequestDto request, String keycloakUserId, String userEmail);

    Page<MaintenanceRecordResponseDto> findByRegister(UUID registerId, UUID assetId, Pageable pageable);

    Page<MaintenanceRecordResponseDto> findByAsset(UUID assetId, Pageable pageable);
}
