package com.proseg.msvcinventory.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvcinventory.dto.request.ExecutingUnitRequestDto;
import com.proseg.msvcinventory.dto.response.ExecutingUnitResponseDto;
import com.proseg.msvcinventory.service.ExecutingUnitService;
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
@RequestMapping("${routes.executing-units:/api/v1/inventory/executing-units}")
@RequiredArgsConstructor
public class ExecutingUnitController {

    private final ExecutingUnitService executingUnitService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExecutingUnitResponseDto>> create(@Valid @RequestBody ExecutingUnitRequestDto request) {
        return ApiResponseBuilder.created(
                executingUnitService.create(request),
                "Unidad ejecutora creada correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExecutingUnitResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                executingUnitService.findById(id),
                "Unidad ejecutora obtenida correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExecutingUnitResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(executingUnitService.findAll(search, filters, pageable)),
                "Lista de unidades ejecutoras"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExecutingUnitResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ExecutingUnitRequestDto request) {

        return ApiResponseBuilder.ok(
                executingUnitService.update(id, request),
                "Unidad ejecutora actualizada correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        executingUnitService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Unidad ejecutora eliminada correctamente"
        );
    }
}
