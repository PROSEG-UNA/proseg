package com.sssi.msvcinventory.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.common.api.util.PageMapper;
import com.sssi.common.specification.FilterConstants;
import com.sssi.msvcinventory.dto.request.EmployeeRequestDto;
import com.sssi.msvcinventory.dto.response.EmployeeResponseDto;
import com.sssi.msvcinventory.service.EmployeeService;
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
@RequestMapping("${routes.employees:/api/v1/inventory/employees}")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> create(@Valid @RequestBody EmployeeRequestDto request) {
        return ApiResponseBuilder.created(
                employeeService.create(request),
                "Funcionario creado correctamente"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                employeeService.findById(id),
                "Funcionario obtenido correctamente"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);

        return ApiResponseBuilder.ok(
                PageMapper.from(employeeService.findAll(search, filters, pageable)),
                "Lista de funcionarios"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeRequestDto request) {

        return ApiResponseBuilder.ok(
                employeeService.update(id, request),
                "Funcionario actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ApiResponseBuilder.ok(
                null,
                "Funcionario eliminado correctamente"
        );
    }
}
