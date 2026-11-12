package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_transport.dto.request.DriverMonthBusinessDaysRequestDto;
import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.service.DriverMonthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.drivers:/api/v1/transport/drivers}/{driverId}/months")
@RequiredArgsConstructor
public class DriverMonthController {

    private final DriverMonthService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverMonthResponseDto>>> findHistory(@PathVariable UUID driverId) {
        return ApiResponseBuilder.ok(service.findHistory(driverId), "Historial mensual del chofer");
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<DriverMonthResponseDto>> findCurrent(@PathVariable UUID driverId) {
        return ApiResponseBuilder.ok(service.findCurrent(driverId), "Registro del mes actual");
    }

    @PutMapping("/{year}/{month}/business-days")
    public ResponseEntity<ApiResponse<DriverMonthResponseDto>> updateBusinessDays(
            @PathVariable UUID driverId,
            @PathVariable int year,
            @PathVariable int month,
            @RequestBody DriverMonthBusinessDaysRequestDto request
    ) {
        return ApiResponseBuilder.ok(
                service.updateBusinessDays(driverId, year, month, request.getBusinessDays()),
                "Días hábiles del chofer actualizados correctamente"
        );
    }

}
