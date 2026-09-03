package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.BuildingRequestDto;
import com.proseg.msvcinventory.dto.response.BuildingResponseDto;
import com.proseg.msvcinventory.entity.Building;
import com.proseg.msvcinventory.entity.Campus;
import com.proseg.msvcinventory.exception.BuildingException;
import com.proseg.msvcinventory.exception.CampusException;
import com.proseg.msvcinventory.mapper.BuildingMapper;
import com.proseg.msvcinventory.repository.BuildingRepository;
import com.proseg.msvcinventory.repository.FloorRepository;
import com.proseg.msvcinventory.repository.CampusRepository;
import com.proseg.msvcinventory.service.BuildingService;
import com.proseg.msvcinventory.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final CampusRepository campusRepository;
    private final FloorRepository floorRepository;
    private final BuildingMapper buildingMapper;

    @Override
    @Transactional
    public BuildingResponseDto create(BuildingRequestDto request) {

        Campus campus = campusRepository.findById(request.getCampusId())
                .orElseThrow(() -> CampusException.notFound(request.getCampusId().toString()));

        if (buildingRepository.existsByNameIgnoreCaseAndCampusId(
                request.getName(), request.getCampusId())) {

            throw BuildingException.duplicateName(request.getName());
        }

        Building building = buildingMapper.toEntity(request);
        building.setCampus(campus);

        return buildingMapper.toResponse(buildingRepository.save(building));
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingResponseDto findById(UUID id) {
        return buildingRepository.findById(id)
                .map(buildingMapper::toResponse)
                .orElseThrow(() -> BuildingException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuildingResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Building> spec = Specification
                .where(GenericSpecifications.<Building>withSearch(Building.class, search))
                .and(GenericSpecifications.<Building>withColumnFilters(Building.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Building.class, pageable.getSort())
        );

        return buildingRepository.findAll(spec, sanitized).map(buildingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuildingResponseDto> findByCampusId(UUID campusId, Pageable pageable) {

        if (!campusRepository.existsById(campusId)) {
            throw CampusException.notFound(campusId.toString());
        }

        return buildingRepository.findByCampusId(campusId, pageable)
                .map(buildingMapper::toResponse);
    }

    @Override
    @Transactional
    public BuildingResponseDto update(UUID id, BuildingRequestDto request) {

        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> BuildingException.notFound(id.toString()));

        Campus campus = campusRepository.findById(request.getCampusId())
                .orElseThrow(() -> CampusException.notFound(request.getCampusId().toString()));

        if (buildingRepository.existsByNameIgnoreCaseAndCampusIdAndIdNot(
                request.getName(), request.getCampusId(), id)) {

            throw BuildingException.duplicateName(request.getName());
        }

        buildingMapper.updateEntityFromRequest(request, building);
        building.setCampus(campus);

        return buildingMapper.toResponse(buildingRepository.save(building));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> BuildingException.notFound(id.toString()));

        if (floorRepository.existsByBuildingId(id)) {
            throw BuildingException.inUse(building.getName());
        }

        buildingRepository.delete(building);
    }
}
