package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.sssi.msvc_maintenance.service.MaintenanceLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    private final MaintenanceLocationService maintenanceLocationService;

    @GetMapping("/campuses")
    public ResponseEntity<ApiResponse<PageResponse<InventoryCampusResponseDto>>> findCampuses(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 200, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findCampuses(search, pageable)),
                "Lista de campus"
        );
    }

    @GetMapping("/campuses/{campusId}/buildings")
    public ResponseEntity<ApiResponse<PageResponse<InventoryBuildingResponseDto>>> findBuildingsByCampus(
            @PathVariable UUID campusId,
            @PageableDefault(size = 200, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceLocationService.findBuildingsByCampus(campusId, pageable)),
                "Edificios del campus"
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

    @GetMapping("/buildings/{id}/emails")
    public ResponseEntity<ApiResponse<List<String>>> findBuildingEmails(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceLocationService.findBuildingEmails(id),
                "Correos electrónicos del edificio obtenidos correctamente"
        );
    }

    @GetMapping("/campuses/{id}/emails")
    public ResponseEntity<ApiResponse<List<String>>> findCampusEmails(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                maintenanceLocationService.findCampusEmails(id),
                "Correos electrónicos del campus obtenidos correctamente"
        );
    }
}
