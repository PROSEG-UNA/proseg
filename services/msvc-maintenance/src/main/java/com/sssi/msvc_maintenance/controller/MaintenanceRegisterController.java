package com.sssi.msvc_maintenance.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRecordRequestDto;
import com.sssi.msvc_maintenance.dto.request.MaintenanceRegisterUpdateRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRegisterResponseDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.sssi.msvc_maintenance.security.Privileges;
import com.sssi.msvc_maintenance.service.MaintenanceRecordService;
import com.sssi.msvc_maintenance.service.MaintenanceRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.registers:/api/v1/maintenance/registers}")
@RequiredArgsConstructor
public class MaintenanceRegisterController {

    private final MaintenanceRegisterService maintenanceRegisterService;
    private final MaintenanceRecordService maintenanceRecordService;

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRequestResponseDto>>> findAssigned(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) MaintenanceStatus status,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRegisterService.findAssigned(jwt.getSubject(), status, pageable)),
                "Solicitudes asignadas"
        );
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRequestResponseDto>>> findHistory(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        boolean companyWide = authentication.getAuthorities().stream()
                .anyMatch(a -> Privileges.RegistrosMantenimiento.HISTORIAL.equals(a.getAuthority()));

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRegisterService.findHistory(jwt.getSubject(), companyWide, pageable)),
                "Historial de registros de mantenimiento"
        );
    }

    @GetMapping("/by-request/{requestId}")
    public ResponseEntity<ApiResponse<MaintenanceRegisterResponseDto>> getOrCreateByRequest(
            @PathVariable UUID requestId) {

        return ApiResponseBuilder.ok(
                maintenanceRegisterService.getOrCreateByRequest(requestId),
                "Registro de mantenimiento obtenido correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceRegisterResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MaintenanceRegisterUpdateRequestDto request) {

        return ApiResponseBuilder.ok(
                maintenanceRegisterService.update(id, request),
                "Registro de mantenimiento actualizado correctamente"
        );
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<MaintenanceRegisterResponseDto>> finalizeRegister(
            @PathVariable UUID id) {

        return ApiResponseBuilder.ok(
                maintenanceRegisterService.finalizeRegister(id),
                "Registro de mantenimiento finalizado correctamente"
        );
    }

    @GetMapping("/{id}/assets")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceAssetOptionDto>>> findRegisterAssets(
            @PathVariable UUID id,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRegisterService.findRegisterAssets(id, search, filters, pageable)),
                "Activos del registro de mantenimiento"
        );
    }

    @PostMapping("/{id}/records")
    public ResponseEntity<ApiResponse<MaintenanceRecordResponseDto>> createRecord(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody MaintenanceRecordRequestDto request) {

        return ApiResponseBuilder.created(
                maintenanceRecordService.create(id, request, jwt.getSubject(), jwt.getClaimAsString("email")),
                "Registro de mantenimiento creado correctamente"
        );
    }

    @GetMapping("/{id}/records")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceRecordResponseDto>>> findRecords(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID assetId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ApiResponseBuilder.ok(
                PageMapper.from(maintenanceRecordService.findByRegister(id, assetId, pageable)),
                "Registros del activo"
        );
    }
}
