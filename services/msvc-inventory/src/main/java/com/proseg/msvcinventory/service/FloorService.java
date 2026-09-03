package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.response.FloorResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FloorService {

    FloorResponseDto findById(UUID id);

    Page<FloorResponseDto> findAll(Pageable pageable);

    Page<FloorResponseDto> findByBuildingId(UUID buildingId, Pageable pageable);
}