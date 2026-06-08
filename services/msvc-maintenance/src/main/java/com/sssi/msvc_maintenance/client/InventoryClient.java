package com.sssi.msvc_maintenance.client;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.config.FeignConfig;
import com.sssi.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.sssi.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "msvc-inventory",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface InventoryClient {

    @GetMapping("/v1/inventory/assets")
    ApiResponse<PageResponse<InventoryAssetResponseDto>> findAssets(
            @RequestParam(required = false) String search,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) List<String> sort
    );

    @GetMapping("/v1/inventory/assets/campus/{campusId}")
    ApiResponse<PageResponse<InventoryAssetResponseDto>> findAssetsByCampus(
            @PathVariable UUID campusId,
            @RequestParam(required = false) String search,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) List<String> sort,
            @SpringQueryMap Map<String, String> filters
    );

    @GetMapping("/v1/inventory/assets/building/{buildingId}")
    ApiResponse<PageResponse<InventoryAssetResponseDto>> findAssetsByBuilding(
            @PathVariable UUID buildingId,
            @RequestParam(required = false) String search,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) List<String> sort,
            @SpringQueryMap Map<String, String> filters
    );

    @GetMapping("/v1/inventory/campuses")
    ApiResponse<PageResponse<InventoryCampusResponseDto>> findCampuses(
            @RequestParam(required = false) String search,
            @RequestParam int page,
            @RequestParam int size
    );

    @GetMapping("/v1/inventory/buildings/campus/{campusId}")
    ApiResponse<PageResponse<InventoryBuildingResponseDto>> findBuildingsByCampus(
            @PathVariable UUID campusId,
            @RequestParam int page,
            @RequestParam int size
    );

    @GetMapping("/v1/inventory/campuses/{id}")
    ApiResponse<InventoryCampusResponseDto> findCampusById(@PathVariable UUID id);

    @GetMapping("/v1/inventory/buildings/{id}")
    ApiResponse<InventoryBuildingResponseDto> findBuildingById(@PathVariable UUID id);
}

