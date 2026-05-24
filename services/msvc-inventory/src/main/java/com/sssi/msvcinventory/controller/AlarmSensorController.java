package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.sssi.msvcinventory.dto.response.AlarmSensorResponseDto;
import com.sssi.msvcinventory.service.AlarmSensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.alarm-sensors:/api/v1/inventory/assets/alarm-sensors}")
@RequiredArgsConstructor
public class AlarmSensorController {

    private final AlarmSensorService alarmSensorService;

    @PostMapping
    public ResponseEntity<ApiResponse<AlarmSensorResponseDto>> create(@Valid @RequestBody AlarmSensorRequestDto request) {
        return ApiResponseBuilder.created(
                alarmSensorService.create(request),
                "Sensor de alarma creado correctamente"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlarmSensorResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AlarmSensorRequestDto request) {
        return ApiResponseBuilder.ok(
                alarmSensorService.update(id, request),
                "Sensor de alarma actualizado correctamente"
        );
    }
}
