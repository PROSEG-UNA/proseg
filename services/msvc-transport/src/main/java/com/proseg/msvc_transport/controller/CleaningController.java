package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftImportResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowDayResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowRequestDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDraftRowResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningExecutionDetailViewResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningExecutionSummaryResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningImportSchemaDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningRegisterResponseDto;
import com.proseg.msvc_transport.dto.cleaning.CleaningVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.service.CleaningDraftService;
import com.proseg.msvc_transport.service.CleaningHistoryService;
import com.proseg.msvc_transport.service.CleaningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.cleaning:/api/v1/transport/cleaning}")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningService cleaningService;
    private final CleaningDraftService cleaningDraftService;
    private final CleaningHistoryService cleaningHistoryService;

    @GetMapping("/imports/schema")
    public ResponseEntity<ApiResponse<CleaningImportSchemaDto>> getImportSchema() {
        return ApiResponseBuilder.ok(cleaningDraftService.getImportSchema(), "Esquema de importación de giras");
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<CleaningDraftImportResponseDto>> createDraft(@Valid @RequestBody CleaningDraftCreateRequestDto request) {
        return ApiResponseBuilder.ok(cleaningDraftService.create(request), "Importación de giras procesada");
    }

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<PageResponse<CleaningDraftResponseDto>>> findDrafts(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(cleaningDraftService.findAll(search, columnFilters(allParams), pageable)),
                "Borradores de depuración"
        );
    }

    @GetMapping("/drafts/{id}")
    public ResponseEntity<ApiResponse<CleaningDraftResponseDto>> findDraftById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(cleaningDraftService.findById(id), "Borrador de depuración");
    }

    @GetMapping("/drafts/{id}/rows")
    public ResponseEntity<ApiResponse<PageResponse<CleaningDraftRowResponseDto>>> findDraftRows(
            @PathVariable UUID id,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(cleaningDraftService.findRows(id, search, columnFilters(allParams), pageable)),
                "Filas del borrador de depuración"
        );
    }

    @PostMapping("/drafts/{id}/rows")
    public ResponseEntity<ApiResponse<CleaningDraftRowResponseDto>> createDraftRow(
            @PathVariable UUID id,
            @RequestBody CleaningDraftRowRequestDto request
    ) {
        return ApiResponseBuilder.created(
                cleaningDraftService.createRow(id, request),
                "Fila agregada al borrador de depuración"
        );
    }

    @PutMapping("/drafts/{id}/rows/{rowId}")
    public ResponseEntity<ApiResponse<CleaningDraftRowResponseDto>> updateDraftRow(
            @PathVariable UUID id,
            @PathVariable UUID rowId,
            @RequestBody CleaningDraftRowRequestDto request
    ) {
        return ApiResponseBuilder.ok(
                cleaningDraftService.updateRow(id, rowId, request),
                "Fila del borrador de depuración actualizada"
        );
    }

    @PutMapping("/drafts/{id}/rows/{rowId}/days/{dayId}")
    public ResponseEntity<ApiResponse<CleaningDraftRowDayResponseDto>> updateDraftRowDay(
            @PathVariable UUID id,
            @PathVariable UUID rowId,
            @PathVariable UUID dayId,
            @Valid @RequestBody CleaningDraftRowDayRequestDto request
    ) {
        return ApiResponseBuilder.ok(
                cleaningDraftService.updateRowDay(id, rowId, dayId, request),
                "Día de la gira actualizado"
        );
    }

    @DeleteMapping("/drafts/{id}/rows/{rowId}")
    public ResponseEntity<ApiResponse<Void>> deleteDraftRow(@PathVariable UUID id, @PathVariable UUID rowId) {
        cleaningDraftService.deleteRow(id, rowId);
        return ApiResponseBuilder.ok(null, "Fila del borrador de depuración eliminada");
    }

    @DeleteMapping("/drafts/{id}/duplicates")
    public ResponseEntity<ApiResponse<Integer>> deleteDraftDuplicates(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                cleaningDraftService.deleteDuplicateRows(id),
                "Filas repetidas del borrador eliminadas"
        );
    }

    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(@PathVariable UUID id) {
        cleaningDraftService.delete(id);
        return ApiResponseBuilder.ok(null, "Borrador de depuración eliminado");
    }

    @GetMapping("/drafts/{id}/drivers/schedule")
    public ResponseEntity<ApiResponse<OvertimeRequestReportDto>> findDraftDriverSchedule(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(cleaningDraftService.getSchedule(id), "Horario de choferes del borrador");
    }

    @GetMapping("/drafts/{id}/drivers/schedule-summary")
    public ResponseEntity<ApiResponse<OvertimeRequestReportDto>> findDraftDriverScheduleSummary(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(cleaningDraftService.getDriverScheduleSummaries(id), "Resumen de sobresueldo por chofer del borrador");
    }

    @GetMapping("/drafts/{id}/drivers/{driverId}/schedule")
    public ResponseEntity<ApiResponse<PageResponse<OvertimeRequestReportDto.TourBlock>>> findDraftDriverTours(
            @PathVariable UUID id,
            @PathVariable UUID driverId,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(cleaningDraftService.getDriverSchedule(id, driverId, pageable)),
                "Giras del chofer en el borrador"
        );
    }

    @PostMapping("/drafts/{id}/drivers/auto-assign")
    public ResponseEntity<ApiResponse<CleaningDriverAutoAssignResponseDto>> autoAssignDraftDrivers(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                cleaningDraftService.autoAssignDrivers(id),
                "Choferes autoasignados"
        );
    }

    @PostMapping("/drafts/{id}/vehicles/auto-assign")
    public ResponseEntity<ApiResponse<CleaningVehicleAutoAssignResponseDto>> autoAssignDraftVehicles(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                cleaningDraftService.autoAssignVehicles(id),
                "Vehículos autoasignados"
        );
    }

    @PostMapping("/drafts/{id}/register")
    public ResponseEntity<ApiResponse<CleaningRegisterResponseDto>> registerDraft(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean replaceExistingInRange
    ) {
        return ApiResponseBuilder.ok(
                cleaningService.registerDraft(id, replaceExistingInRange),
                "Giras registradas correctamente"
        );
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<CleaningExecutionSummaryResponseDto>>> findHistory(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(cleaningHistoryService.findAll(search, columnFilters(allParams), pageable)),
                "Historial de depuraciones"
        );
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<CleaningExecutionDetailViewResponseDto>> findHistoryById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(cleaningHistoryService.findById(id), "Detalle de depuración");
    }

    private Map<String, String> columnFilters(Map<String, String> allParams) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return filters;
    }
}
