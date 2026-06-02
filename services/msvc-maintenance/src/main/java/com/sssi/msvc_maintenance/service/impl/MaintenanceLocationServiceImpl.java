package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.client.InventoryClient;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.sssi.msvc_maintenance.service.MaintenanceLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceLocationServiceImpl implements MaintenanceLocationService {

    private final InventoryClient inventoryClient;

    @Override
    public Page<InventoryCampusResponseDto> findCampuses(String search, Pageable pageable) {
        ApiResponse<PageResponse<InventoryCampusResponseDto>> response = inventoryClient.findCampuses(
                search, pageable.getPageNumber(), pageable.getPageSize()
        );
        PageResponse<InventoryCampusResponseDto> data = response != null ? response.getData() : null;
        if (data == null) return new PageImpl<>(List.of(), pageable, 0);
        List<InventoryCampusResponseDto> content = data.getContent() != null ? data.getContent() : List.of();
        return new PageImpl<>(content, pageable, data.getTotalElements());
    }

    @Override
    public Page<InventoryBuildingResponseDto> findBuildingsByCampus(UUID campusId, Pageable pageable) {
        ApiResponse<PageResponse<InventoryBuildingResponseDto>> response = inventoryClient.findBuildingsByCampus(
                campusId, pageable.getPageNumber(), pageable.getPageSize()
        );
        PageResponse<InventoryBuildingResponseDto> data = response != null ? response.getData() : null;
        if (data == null) return new PageImpl<>(List.of(), pageable, 0);
        List<InventoryBuildingResponseDto> content = data.getContent() != null ? data.getContent() : List.of();
        return new PageImpl<>(content, pageable, data.getTotalElements());
    }

    @Override
    public InventoryCampusResponseDto findCampusById(UUID id) {
        ApiResponse<InventoryCampusResponseDto> response = inventoryClient.findCampusById(id);
        return response != null ? response.getData() : null;
    }

    @Override
    public InventoryBuildingResponseDto findBuildingById(UUID id) {
        ApiResponse<InventoryBuildingResponseDto> response = inventoryClient.findBuildingById(id);
        return response != null ? response.getData() : null;
    }
}
