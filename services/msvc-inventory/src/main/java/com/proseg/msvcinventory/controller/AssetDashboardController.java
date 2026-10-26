package com.proseg.msvcinventory.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.msvcinventory.dto.response.AssetDashboardResponseDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.entity.enums.AssetExpiryCategory;
import com.proseg.msvcinventory.service.AssetDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${routes.assets:/api/v1/inventory/assets}/dashboard")
@RequiredArgsConstructor
public class AssetDashboardController {

    private final AssetDashboardService assetDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<AssetDashboardResponseDto>> getSummary(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponseBuilder.ok(assetDashboardService.getSummary(days), "Resumen de activos");
    }

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponseDto>>> findExpiringAssets(
            @RequestParam AssetExpiryCategory category,
            @RequestParam(defaultValue = "30") int days,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(assetDashboardService.findExpiringAssets(category, days, pageable)),
                "Activos por vencimiento"
        );
    }
}
