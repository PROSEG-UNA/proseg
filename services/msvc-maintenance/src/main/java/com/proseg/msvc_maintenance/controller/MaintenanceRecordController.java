package com.proseg.msvc_maintenance.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.proseg.msvc_maintenance.service.MaintenanceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${routes.records:/api/v1/maintenance/records}")
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRecordResponseDto>>> findByAsset(
            @RequestParam UUID assetId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRecordService.findByAsset(assetId, pageable)),
                "Historial de mantenimiento del activo"
        );
    }
}
