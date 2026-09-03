package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.proseg.msvc_maintenance.service.MaintenanceLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceLocationServiceImpl implements MaintenanceLocationService {

    private final InventoryClient inventoryClient;

    @Override
    public Page<InventoryCampusResponseDto> findCampuses(String search, Pageable pageable) {
        ApiResponse<PageResponse<InventoryCampusResponseDto>> response = inventoryClient.findCampuses(
                search,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return toPage(response, pageable);
    }

    @Override
    public Page<InventoryBuildingResponseDto> findBuildingsByCampus(UUID campusId, Pageable pageable) {
        ApiResponse<PageResponse<InventoryBuildingResponseDto>> response = inventoryClient.findBuildingsByCampus(
                campusId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return toPage(response, pageable);
    }

    @Override
    public Page<InventoryAssetFloorResponseDto> findFloorsByBuilding(UUID buildingId, Pageable pageable) {
        ApiResponse<PageResponse<InventoryAssetFloorResponseDto>> response = inventoryClient.findFloorsByBuilding(
                buildingId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return toPage(response, pageable);
    }

    @Override
    public Page<InventoryAssetLocationResponseDto> findLocationsByBuilding(UUID buildingId, Pageable pageable) {
        ApiResponse<PageResponse<InventoryAssetLocationResponseDto>> response = inventoryClient.findLocationsByBuilding(
                buildingId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return toPage(response, pageable);
    }

    @Override
    public Page<InventoryAssetLocationResponseDto> findLocationsByCampus(UUID campusId, Pageable pageable) {
        ApiResponse<PageResponse<InventoryAssetLocationResponseDto>> response = inventoryClient.findLocationsByCampus(
                campusId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return toPage(response, pageable);
    }

    @Override
    public Page<InventoryAssetResponseDto> findAssetsByBuilding(UUID buildingId, String search, Pageable pageable) {
        ApiResponse<PageResponse<InventoryAssetResponseDto>> response = inventoryClient.findAssetsByBuilding(
                buildingId,
                search,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                null,
                Map.of()
        );

        return toPage(response, pageable);
    }

    @Override
    public Page<InventoryAssetResponseDto> findAssetsByLocation(UUID locationId, String search, Pageable pageable) {
        ApiResponse<PageResponse<InventoryAssetResponseDto>> response = inventoryClient.findAssetsByLocation(
                locationId,
                search,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return toPage(response, pageable);
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

    @Override
    public InventoryAssetFloorResponseDto findFloorById(UUID id) {
        ApiResponse<InventoryAssetFloorResponseDto> response = inventoryClient.findFloorById(id);
        return response != null ? response.getData() : null;
    }

    @Override
    public InventoryAssetLocationResponseDto findLocationById(UUID id) {
        ApiResponse<InventoryAssetLocationResponseDto> response = inventoryClient.findLocationById(id);
        return response != null ? response.getData() : null;
    }

    @Override
    public List<String> findBuildingEmails(UUID buildingId, Pageable pageable) {
        return extractEmails(inventoryClient.findBuildingEmails(buildingId));
    }

    @Override
    public List<String> findCampusEmails(UUID campusId, Pageable pageable) {
        return extractEmails(inventoryClient.findCampusEmails(campusId));
    }

    private <T> Page<T> toPage(ApiResponse<PageResponse<T>> response, Pageable pageable) {
        PageResponse<T> data = response != null ? response.getData() : null;
        if (data == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<T> content = data.getContent() != null ? data.getContent() : List.of();
        return new PageImpl<>(content, pageable, data.getTotalElements());
    }

    private List<String> extractEmails(ApiResponse<List<InventoryBuildingEmailResponseDto>> response) {
        List<InventoryBuildingEmailResponseDto> data = response != null ? response.getData() : null;
        if (data == null) {
            return List.of();
        }

        return data.stream()
                .map(InventoryBuildingEmailResponseDto::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
    }
}