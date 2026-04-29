package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.LocationRequestDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LocationService {

    LocationResponseDto create(LocationRequestDto request);

    LocationResponseDto findById(UUID id);

    Page<LocationResponseDto> findAll(Pageable pageable);

    Page<LocationResponseDto> findBySiteId(UUID siteId, Pageable pageable);

    LocationResponseDto update(UUID id, LocationRequestDto request);

    void delete(UUID id);
}
