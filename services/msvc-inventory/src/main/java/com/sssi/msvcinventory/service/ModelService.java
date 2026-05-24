package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.ModelRequestDto;
import com.sssi.msvcinventory.dto.response.ModelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ModelService {

    ModelResponseDto create(ModelRequestDto request);

    ModelResponseDto findById(UUID id);

    Page<ModelResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<ModelResponseDto> findByBrandId(UUID brandId, Pageable pageable);

    Page<ModelResponseDto> findByTypeId(UUID typeId, Pageable pageable);

    ModelResponseDto update(UUID id, ModelRequestDto request);

    void delete(UUID id);
}
