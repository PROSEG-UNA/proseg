package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MaintenanceLocationService {

    Page<InventoryCampusResponseDto> findCampuses(String search, Pageable pageable);

    Page<InventoryBuildingResponseDto> findBuildingsByCampus(UUID campusId, Pageable pageable);

    InventoryCampusResponseDto findCampusById(UUID id);

    InventoryBuildingResponseDto findBuildingById(UUID id);
}
