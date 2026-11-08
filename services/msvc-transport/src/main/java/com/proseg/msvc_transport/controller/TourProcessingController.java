package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftCreateRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftImportResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowApprovalRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowDayResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowRequestDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingImportSchemaDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionCheckDto;
import com.proseg.msvc_transport.dto.tourprocessing.FinalCommissionReportDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourControlReportDto;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.service.TourProcessingDraftService;
import com.proseg.msvc_transport.service.FinalCommissionService;
import com.proseg.msvc_transport.service.TourControlReportService;
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
@RequestMapping("${routes.tour-processing:/api/v1/transport/tour-processing}")
@RequiredArgsConstructor
public class TourProcessingController {

    private final TourProcessingDraftService tourProcessingDraftService;
    private final FinalCommissionService finalCommissionService;
    private final TourControlReportService tourControlReportService;

    @GetMapping("/imports/schema")
    public ResponseEntity<ApiResponse<TourProcessingImportSchemaDto>> getImportSchema() {
        return ApiResponseBuilder.ok(tourProcessingDraftService.getImportSchema(), "Esquema de importación de giras");
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<TourProcessingDraftImportResponseDto>> createDraft(@Valid @RequestBody TourProcessingDraftCreateRequestDto request) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.create(request), "Importación de giras procesada");
    }

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<PageResponse<TourProcessingDraftResponseDto>>> findDrafts(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(tourProcessingDraftService.findAll(search, columnFilters(allParams), pageable)),
                "Borradores de depuración"
        );
    }

    @GetMapping("/drafts/{id}")
    public ResponseEntity<ApiResponse<TourProcessingDraftResponseDto>> findDraftById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.findById(id), "Borrador de depuración");
    }

    @PostMapping("/drafts/{id}/finish")
    public ResponseEntity<ApiResponse<TourProcessingDraftResponseDto>> finishDraft(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.finish(id), "Depuración terminada");
    }

    @PostMapping("/drafts/{id}/reopen")
    public ResponseEntity<ApiResponse<TourProcessingDraftResponseDto>> reopenDraft(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.reopen(id), "Depuración devuelta a borrador");
    }

    @GetMapping("/drafts/{id}/rows")
    public ResponseEntity<ApiResponse<PageResponse<TourProcessingDraftRowResponseDto>>> findDraftRows(
            @PathVariable UUID id,
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(tourProcessingDraftService.findRows(id, search, columnFilters(allParams), pageable)),
                "Filas del borrador de depuración"
        );
    }

    @GetMapping("/drafts/{id}/rows/{rowId}")
    public ResponseEntity<ApiResponse<TourProcessingDraftRowResponseDto>> findDraftRow(@PathVariable UUID id, @PathVariable UUID rowId) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.findRow(id, rowId), "Fila del borrador de depuración");
    }

    @PostMapping("/drafts/{id}/rows")
    public ResponseEntity<ApiResponse<TourProcessingDraftRowResponseDto>> createDraftRow(
            @PathVariable UUID id,
            @RequestBody TourProcessingDraftRowRequestDto request
    ) {
        return ApiResponseBuilder.created(
                tourProcessingDraftService.createRow(id, request),
                "Fila agregada al borrador de depuración"
        );
    }

    @PutMapping("/drafts/{id}/rows/{rowId}")
    public ResponseEntity<ApiResponse<TourProcessingDraftRowResponseDto>> updateDraftRow(
            @PathVariable UUID id,
            @PathVariable UUID rowId,
            @RequestBody TourProcessingDraftRowRequestDto request
    ) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.updateRow(id, rowId, request),
                "Fila del borrador de depuración actualizada"
        );
    }

    @PatchMapping("/drafts/{id}/rows/{rowId}/approval-status")
    public ResponseEntity<ApiResponse<TourProcessingDraftRowResponseDto>> updateDraftRowApprovalStatus(
            @PathVariable UUID id,
            @PathVariable UUID rowId,
            @Valid @RequestBody TourProcessingDraftRowApprovalRequestDto request
    ) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.updateRowApprovalStatus(id, rowId, request.getApprovalStatus()),
                "Estado de la gira actualizado"
        );
    }

    @PutMapping("/drafts/{id}/rows/{rowId}/days/{dayId}")
    public ResponseEntity<ApiResponse<TourProcessingDraftRowDayResponseDto>> updateDraftRowDay(
            @PathVariable UUID id,
            @PathVariable UUID rowId,
            @PathVariable UUID dayId,
            @Valid @RequestBody TourProcessingDraftRowDayRequestDto request
    ) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.updateRowDay(id, rowId, dayId, request),
                "Día de la gira actualizado"
        );
    }

    @DeleteMapping("/drafts/{id}/rows/{rowId}")
    public ResponseEntity<ApiResponse<Void>> deleteDraftRow(@PathVariable UUID id, @PathVariable UUID rowId) {
        tourProcessingDraftService.deleteRow(id, rowId);
        return ApiResponseBuilder.ok(null, "Fila del borrador de depuración eliminada");
    }

    @DeleteMapping("/drafts/{id}/duplicates")
    public ResponseEntity<ApiResponse<Integer>> deleteDraftDuplicates(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.deleteDuplicateRows(id),
                "Filas repetidas del borrador eliminadas"
        );
    }

    @PostMapping("/drafts/{id}/split-oversized")
    public ResponseEntity<ApiResponse<Integer>> splitDraftOversizedRows(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "DISTINCT_VEHICLES") TourSplitMode mode
    ) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.splitOversizedRows(id, mode),
                "Giras divididas por capacidad de vehículo"
        );
    }

    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(@PathVariable UUID id) {
        tourProcessingDraftService.delete(id);
        return ApiResponseBuilder.ok(null, "Borrador de depuración eliminado");
    }

    @PostMapping("/drafts/{id}/assignments/clear")
    public ResponseEntity<ApiResponse<Void>> clearDraftAssignments(@PathVariable UUID id) {
        tourProcessingDraftService.clearAssignments(id);
        return ApiResponseBuilder.ok(null, "Asignaciones de choferes y vehículos eliminadas");
    }

    @GetMapping("/drafts/{id}/drivers/schedule")
    public ResponseEntity<ApiResponse<OvertimeRequestReportDto>> findDraftDriverSchedule(@PathVariable UUID id, @RequestParam(required = false) java.time.LocalDate date) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.getSchedule(id, date), "Horario de choferes del borrador");
    }

    @GetMapping("/drafts/{id}/drivers/schedule-summary")
    public ResponseEntity<ApiResponse<OvertimeRequestReportDto>> findDraftDriverScheduleSummary(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(tourProcessingDraftService.getDriverScheduleSummaries(id), "Resumen de sobresueldo por chofer del borrador");
    }

    @GetMapping("/drafts/{id}/drivers/{driverId}/schedule")
    public ResponseEntity<ApiResponse<PageResponse<OvertimeRequestReportDto.TourBlock>>> findDraftDriverTours(
            @PathVariable UUID id,
            @PathVariable UUID driverId,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return ApiResponseBuilder.ok(
                PageMapper.from(tourProcessingDraftService.getDriverSchedule(id, driverId, pageable)),
                "Giras del chofer en el borrador"
        );
    }

    @PostMapping("/drafts/{id}/drivers/auto-assign")
    public ResponseEntity<ApiResponse<TourProcessingDriverAutoAssignResponseDto>> autoAssignDraftDrivers(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.autoAssignDrivers(id),
                "Choferes autoasignados"
        );
    }

    @PostMapping("/drafts/{id}/drivers/auto-assign-ulloa")
    public ResponseEntity<ApiResponse<TourProcessingDriverAutoAssignResponseDto>> autoAssignDraftUlloaDrivers(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.autoAssignUlloaDrivers(id),
                "Choferes de Periféricas autoasignados"
        );
    }

    @PostMapping("/drafts/{id}/vehicles/auto-assign")
    public ResponseEntity<ApiResponse<TourProcessingVehicleAutoAssignResponseDto>> autoAssignDraftVehicles(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                tourProcessingDraftService.autoAssignVehicles(id),
                "Vehículos autoasignados"
        );
    }

    @GetMapping("/drafts/{id}/final-commission/check")
    public ResponseEntity<ApiResponse<FinalCommissionCheckDto>> checkDraftFinalCommission(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(finalCommissionService.check(id), "Revisión de la Comisión Final");
    }

    @PostMapping("/drafts/{id}/final-commission/reject-incomplete")
    public ResponseEntity<ApiResponse<Integer>> rejectDraftIncompleteTours(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(finalCommissionService.rejectIncompleteTours(id), "Giras sin chofer o vehículo marcadas como no aprobadas");
    }

    @GetMapping("/drafts/{id}/final-commission")
    public ResponseEntity<ApiResponse<FinalCommissionReportDto>> findDraftFinalCommission(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(finalCommissionService.report(id), "Datos de la Comisión Final");
    }

    @GetMapping("/drafts/{id}/tour-control")
    public ResponseEntity<ApiResponse<TourControlReportDto>> findDraftTourControl(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(tourControlReportService.report(id), "Datos del Control de Giras");
    }

    @GetMapping("/drafts/months/{year}/{month}/tour-control")
    public ResponseEntity<ApiResponse<TourControlReportDto>> findMonthTourControl(@PathVariable int year, @PathVariable int month) {
        return ApiResponseBuilder.ok(tourControlReportService.reportForMonth(year, month), "Datos del Control de Giras");
    }

    private Map<String, String> columnFilters(Map<String, String> allParams) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return filters;
    }
}
