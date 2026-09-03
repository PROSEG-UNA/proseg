package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.BuildingEmailRequestDto;
import com.proseg.msvcinventory.dto.response.BuildingEmailResponseDto;

import java.util.List;
import java.util.UUID;

public interface BuildingEmailService {

    BuildingEmailResponseDto create(UUID buildingId, BuildingEmailRequestDto request);

    List<BuildingEmailResponseDto> findByBuildingId(UUID buildingId);

    List<BuildingEmailResponseDto> findByCampusId(UUID campusId);

    void delete(UUID emailId);
}