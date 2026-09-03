package com.proseg.msvcinventory.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.msvcinventory.dto.response.FloorResponseDto;
import com.proseg.msvcinventory.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${routes.floors:/api/v1/inventory/floors}")
@RequiredArgsConstructor
public class FloorController {

    private final FloorService floorService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FloorResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(floorService.findById(id), "Piso obtenido correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FloorResponseDto>>> findAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(PageMapper.from(floorService.findAll(pageable)), "Lista de pisos");
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<ApiResponse<PageResponse<FloorResponseDto>>> findByBuildingId(
            @PathVariable UUID buildingId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ApiResponseBuilder.ok(
                PageMapper.from(floorService.findByBuildingId(buildingId, pageable)),
                "Pisos por edificio"
        );
    }
}