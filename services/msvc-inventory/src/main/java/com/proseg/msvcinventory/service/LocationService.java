package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.LocationRequestDto;
import com.proseg.msvcinventory.dto.response.LocationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface LocationService {

    LocationResponseDto create(LocationRequestDto request);

    LocationResponseDto findById(UUID id);

    Page<LocationResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<LocationResponseDto> findByCampusId(UUID campusId, Pageable pageable);

    Page<LocationResponseDto> findByBuildingId(UUID buildingId, Pageable pageable);

    LocationResponseDto update(UUID id, LocationRequestDto request);

    void delete(UUID id);
}
