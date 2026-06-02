package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.response.FloorResponseDto;
import com.sssi.msvcinventory.exception.BuildingException;
import com.sssi.msvcinventory.exception.FloorException;
import com.sssi.msvcinventory.mapper.FloorMapper;
import com.sssi.msvcinventory.repository.BuildingRepository;
import com.sssi.msvcinventory.repository.FloorRepository;
import com.sssi.msvcinventory.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;
    private final FloorMapper floorMapper;

    @Override
    @Transactional(readOnly = true)
    public FloorResponseDto findById(java.util.UUID id) {
        return floorRepository.findById(id)
                .map(floorMapper::toResponse)
                .orElseThrow(() -> FloorException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FloorResponseDto> findAll(Pageable pageable) {
        return floorRepository.findAll(pageable).map(floorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FloorResponseDto> findByBuildingId(java.util.UUID buildingId, Pageable pageable) {
        if (!buildingRepository.existsById(buildingId)) {
            throw BuildingException.notFound(buildingId.toString());
        }

        return floorRepository.findAll((root, query, cb) -> cb.equal(root.get("building").get("id"), buildingId), pageable)
                .map(floorMapper::toResponse);
    }
}