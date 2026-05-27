package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.BuildingRequestDto;
import com.sssi.msvcinventory.dto.response.BuildingResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface BuildingService {

    BuildingResponseDto create(BuildingRequestDto request);

    BuildingResponseDto findById(UUID id);

    Page<BuildingResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<BuildingResponseDto> findByCampusId(UUID campusId, Pageable pageable);

    BuildingResponseDto update(UUID id, BuildingRequestDto request);

    void delete(UUID id);
}
