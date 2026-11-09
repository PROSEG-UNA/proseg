package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.common.specification.FilterConstants;
import com.proseg.msvc_transport.dto.request.ObservationKeywordRequestDto;
import com.proseg.msvc_transport.dto.response.ObservationKeywordResponseDto;
import com.proseg.msvc_transport.service.ObservationKeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${routes.observation-keywords:/api/v1/transport/observation-keywords}")
@RequiredArgsConstructor
public class ObservationKeywordController {

    private final ObservationKeywordService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ObservationKeywordResponseDto>> create(@Valid @RequestBody ObservationKeywordRequestDto request) {
        return ApiResponseBuilder.created(service.create(request), "Palabra clave creada correctamente");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ObservationKeywordResponseDto>> findById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(service.findById(id), "Palabra clave obtenida correctamente");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ObservationKeywordResponseDto>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam Map<String, String> allParams,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        Map<String, String> filters = new HashMap<>(allParams);
        FilterConstants.RESERVED_PARAMS.forEach(filters::remove);
        return ApiResponseBuilder.ok(PageMapper.from(service.findAll(search, filters, pageable)), "Lista de palabras clave");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ObservationKeywordResponseDto>> update(@PathVariable UUID id, @Valid @RequestBody ObservationKeywordRequestDto request) {
        return ApiResponseBuilder.ok(service.update(id, request), "Palabra clave actualizada correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponseBuilder.ok(null, "Palabra clave eliminada correctamente");
    }
}
