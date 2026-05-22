package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface MaintenanceRequestService {

    MaintenanceRequestResponseDto create(MaintenanceRequestRequestDto request);

    MaintenanceRequestResponseDto findById(UUID id);

    Page<MaintenanceRequestResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<MaintenanceRequestResponseDto> findByCompanyId(UUID companyId, Pageable pageable);

    MaintenanceRequestResponseDto update(UUID id, MaintenanceRequestRequestDto request);

    void delete(UUID id);
}

