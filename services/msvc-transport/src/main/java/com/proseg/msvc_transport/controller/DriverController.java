package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.DriverRequestDto;
import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.dto.response.DriverMonthSurplusCapResponseDto;
import com.proseg.msvc_transport.dto.response.DriverResponseDto;
import com.proseg.msvc_transport.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_transport.service.DriverMonthService;
import com.proseg.msvc_transport.service.DriverService;
import com.proseg.msvc_transport.service.OvertimeRequestReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.drivers:/api/v1/transport/drivers}")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService service;
    private final DriverMonthService driverMonthService;
    private final OvertimeRequestReportService overtimeRequestReportService;

    @GetMapping("/months")
    public ResponseEntity<ApiResponse<List<DriverMonthResponseDto>>> findMonthsForPeriod(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponseBuilder.ok(driverMonthService.findAllForPeriod(year, month), "Registros mensuales del periodo");
    }

    @GetMapping("/months/surplus-cap")
    public ResponseEntity<ApiResponse<DriverMonthSurplusCapResponseDto>> findSurplusCapForPeriod(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponseBuilder.ok(driverMonthService.findSurplusCap(year, month), "Horas de sobresueldo del periodo");
    }

    @GetMapping("/months/{year}/{month}/overtime-request")
    public ResponseEntity<ApiResponse<OvertimeRequestReportDto>> findOvertimeRequest(
            @PathVariable int year,
            @PathVariable int month
    ) {
        return ApiResponseBuilder.ok(overtimeRequestReportService.build(year, month), "Solicitud de horas extra del periodo");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponseDto>> create(@Valid @RequestBody DriverRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Chofer creado correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Chofer obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DriverResponseDto>>> findAll(@RequestParam(required = false) String search, @RequestParam Map<String, String> allParams, @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de choferes");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody DriverRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Chofer actualizado correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Chofer eliminado correctamente");
    }
}
