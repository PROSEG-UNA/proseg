package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.sssi.msvc_maintenance.service.MaintenanceLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.maintenance-locations:/api/v1/maintenance/locations}")
@RequiredArgsConstructor
public class MaintenanceLocationController {

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "200";

    private final MaintenanceLocationService maintenanceLocationService;

    @GetMapping("/campuses")
    public ResponseEntity<ApiResponse<PageResponse<InventoryCampusResponseDto>>> findCampuses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findCampuses(search, pageable)),
                "Lista de campus"
        );
    }

    @GetMapping("/campuses/{campusId}/buildings")
    public ResponseEntity<ApiResponse<PageResponse<InventoryBuildingResponseDto>>> findBuildingsByCampus(
            @PathVariable UUID campusId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findBuildingsByCampus(campusId, pageable)),
                "Edificios del campus"
        );
    }

    @GetMapping("/buildings/{buildingId}/floors")
    public ResponseEntity<ApiResponse<PageResponse<InventoryAssetFloorResponseDto>>> findFloorsByBuilding(
            @PathVariable UUID buildingId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findFloorsByBuilding(buildingId, pageable)),
                "Pisos del edificio"
        );
    }

    @GetMapping("/buildings/{buildingId}/locations")
    public ResponseEntity<ApiResponse<PageResponse<InventoryAssetLocationResponseDto>>> findLocationsByBuilding(
            @PathVariable UUID buildingId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findLocationsByBuilding(buildingId, pageable)),
                "Ubicaciones del edificio"
        );
    }

    @GetMapping("/campuses/{campusId}/locations")
    public ResponseEntity<ApiResponse<PageResponse<InventoryAssetLocationResponseDto>>> findLocationsByCampus(
            @PathVariable UUID campusId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findLocationsByCampus(campusId, pageable)),
                "Ubicaciones del campus"
        );
    }

    @GetMapping("/campuses/{id}")
    public ResponseEntity<ApiResponse<InventoryCampusResponseDto>> findCampusById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceLocationService.findCampusById(id),
                "Campus obtenido correctamente"
        );
    }

    @GetMapping("/buildings/{id}")
    public ResponseEntity<ApiResponse<InventoryBuildingResponseDto>> findBuildingById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceLocationService.findBuildingById(id),
                "Edificio obtenido correctamente"
        );
    }

    @GetMapping("/floors/{id}")
    public ResponseEntity<ApiResponse<InventoryAssetFloorResponseDto>> findFloorById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceLocationService.findFloorById(id),
                "Piso obtenido correctamente"
        );
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<ApiResponse<InventoryAssetLocationResponseDto>> findLocationById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceLocationService.findLocationById(id),
                "Ubicación obtenida correctamente"
        );
    }

    @GetMapping("/locations/{locationId}/assets")
    public ResponseEntity<ApiResponse<PageResponse<InventoryAssetResponseDto>>> findAssetsByLocation(
            @PathVariable UUID locationId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findAssetsByLocation(locationId, search, pageable)),
                "Activos de la ubicación"
        );
    }

        @GetMapping("/buildings/{buildingId}/assets")
        public ResponseEntity<ApiResponse<PageResponse<InventoryAssetResponseDto>>> findAssetsByBuilding(
                        @PathVariable UUID buildingId,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = DEFAULT_PAGE) int page,
                        @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

                Pageable pageable = PageRequest.of(page, size);

                return ApiResponseBuilder.ok(
                                PageMapper.from(maintenanceLocationService.findAssetsByBuilding(buildingId, search, pageable)),
                                "Activos del edificio"
                );
        }

    @GetMapping("/buildings/{id}/emails")
    public ResponseEntity<ApiResponse<List<String>>> findBuildingEmails(
            @PathVariable UUID id,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                maintenanceLocationService.findBuildingEmails(id, pageable),
                "Correos electrónicos del edificio obtenidos correctamente"
        );
    }

    @GetMapping("/campuses/{id}/emails")
    public ResponseEntity<ApiResponse<List<String>>> findCampusEmails(
            @PathVariable UUID id,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponseBuilder.ok(
                maintenanceLocationService.findCampusEmails(id, pageable),
                "Correos electrónicos del campus obtenidos correctamente"
        );
    }
}