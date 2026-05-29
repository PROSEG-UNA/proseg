package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.MaintenanceTechnicianRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceTechnicianResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface MaintenanceTechnicianService {

    MaintenanceTechnicianResponseDto create(UUID maintenanceRequestId, MaintenanceTechnicianRequestDto request);

    MaintenanceTechnicianResponseDto findById(UUID id);

    Page<MaintenanceTechnicianResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<MaintenanceTechnicianResponseDto> findByMaintenanceRequestId(UUID maintenanceRequestId, Pageable pageable);

    MaintenanceTechnicianResponseDto update(UUID id, MaintenanceTechnicianRequestDto request);

    void delete(UUID id);
}

