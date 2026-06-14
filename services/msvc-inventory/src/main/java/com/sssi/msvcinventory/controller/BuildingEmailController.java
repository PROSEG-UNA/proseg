package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvcinventory.dto.request.BuildingEmailRequestDto;
import com.sssi.msvcinventory.dto.response.BuildingEmailResponseDto;
import com.sssi.msvcinventory.service.BuildingEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BuildingEmailController {

    private final BuildingEmailService buildingEmailService;

    @PostMapping("${routes.buildings:/api/v1/inventory/buildings}/{id}/emails")
    public ResponseEntity<ApiResponse<BuildingEmailResponseDto>> create(
            @PathVariable UUID id,
            @Valid @RequestBody BuildingEmailRequestDto request) {
        return ApiResponseBuilder.created(
                buildingEmailService.create(id, request),
                "Correo electrónico registrado correctamente"
        );
    }

    @GetMapping("${routes.buildings:/api/v1/inventory/buildings}/{id}/emails")
    public ResponseEntity<ApiResponse<List<BuildingEmailResponseDto>>> findByBuilding(
            @PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                buildingEmailService.findByBuildingId(id),
                "Correos electrónicos del edificio obtenidos correctamente"
        );
    }

    @GetMapping("${routes.campuses:/api/v1/inventory/campuses}/{id}/emails")
    public ResponseEntity<ApiResponse<List<BuildingEmailResponseDto>>> findByCampus(
            @PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                buildingEmailService.findByCampusId(id),
                "Correos electrónicos del campus obtenidos correctamente"
        );
    }

    @DeleteMapping("${routes.building-emails:/api/v1/inventory/emails}/{emailId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID emailId) {
        buildingEmailService.delete(emailId);
        return ApiResponseBuilder.ok(null, "Correo electrónico eliminado correctamente");
    }
}