package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvcinventory.dto.request.CampusRequestDto;
import com.sssi.msvcinventory.dto.response.CampusResponseDto;
import com.sssi.msvcinventory.service.CampusService;
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
@RequestMapping("${routes.campuses:/api/v1/inventory/campuses}")
@RequiredArgsConstructor
public class CampusController {

    private final CampusService campusService;

    @PostMapping
    public ResponseEntity<ApiResponse<CampusResponseDto>> create(@Valid @RequestBody CampusRequestDto request) {
        return ApiResponseBuilder.created(
                campusService.create(request),
                "Campus creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampusResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                campusService.findById(id),
                "Campus obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CampusResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(campusService.findAll(search, filters, pageable)),
                "Lista de campus"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CampusResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CampusRequestDto request) {

        return ApiResponseBuilder.ok(
                campusService.update(id, request),
                "Campus actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        campusService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Campus eliminado correctamente"
        );
    }
}
