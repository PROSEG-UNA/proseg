package com.sssi.msvc_maintenance.service;

import com.sssi.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MaintenanceLocationService {

    Page<InventoryCampusResponseDto> findCampuses(String search, Pageable pageable);

    Page<InventoryBuildingResponseDto> findBuildingsByCampus(UUID campusId, Pageable pageable);

    Page<InventoryAssetFloorResponseDto> findFloorsByBuilding(UUID buildingId, Pageable pageable);

    Page<InventoryAssetLocationResponseDto> findLocationsByBuilding(UUID buildingId, Pageable pageable);

    Page<InventoryAssetLocationResponseDto> findLocationsByCampus(UUID campusId, Pageable pageable);

    Page<InventoryAssetResponseDto> findAssetsByBuilding(UUID buildingId, String search, Pageable pageable);

    Page<InventoryAssetResponseDto> findAssetsByLocation(UUID locationId, String search, Pageable pageable);

    InventoryCampusResponseDto findCampusById(UUID id);

    InventoryBuildingResponseDto findBuildingById(UUID id);

    InventoryAssetFloorResponseDto findFloorById(UUID id);

    InventoryAssetLocationResponseDto findLocationById(UUID id);

    List<String> findBuildingEmails(UUID buildingId, Pageable pageable);

    List<String> findCampusEmails(UUID campusId, Pageable pageable);
}