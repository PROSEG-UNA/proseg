package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.LocationRequestDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import com.sssi.msvcinventory.entity.Building;
import com.sssi.msvcinventory.entity.Floor;
import com.sssi.msvcinventory.entity.Location;
import com.sssi.msvcinventory.exception.BuildingException;
import com.sssi.msvcinventory.exception.CampusException;
import com.sssi.msvcinventory.exception.LocationException;
import com.sssi.msvcinventory.mapper.LocationMapper;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.BuildingRepository;
import com.sssi.msvcinventory.repository.FloorRepository;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.repository.CampusRepository;
import com.sssi.msvcinventory.service.LocationService;
import com.sssi.msvcinventory.specification.GenericSpecifications;
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
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final CampusRepository campusRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final AssetRepository assetRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public LocationResponseDto create(LocationRequestDto request) {

        Floor floor = resolveFloor(request);

        if (locationRepository.existsByDescriptionIgnoreCaseAndFloorId(
                request.getDescription(), floor.getId())) {

            throw LocationException.duplicateDescription(request.getDescription());
        }

        Location location = locationMapper.toEntity(request);
        location.setFloor(floor);

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponseDto findById(UUID id) {
        return locationRepository.findById(id)
                .map(locationMapper::toResponse)
                .orElseThrow(() -> LocationException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Location> spec = Specification
                .where(GenericSpecifications.<Location>withSearch(Location.class, search))
                .and(GenericSpecifications.<Location>withColumnFilters(Location.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Location.class, pageable.getSort())
        );

        return locationRepository.findAll(spec, sanitized).map(locationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponseDto> findByCampusId(UUID campusId, Pageable pageable) {

        if (!campusRepository.existsById(campusId)) {
            throw CampusException.notFound(campusId.toString());
        }

        return locationRepository.findByFloorBuildingCampusId(campusId, pageable)
                .map(locationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponseDto> findByBuildingId(UUID buildingId, Pageable pageable) {

        if (!buildingRepository.existsById(buildingId)) {
            throw BuildingException.notFound(buildingId.toString());
        }

        return locationRepository.findByFloorBuildingId(buildingId, pageable)
                .map(locationMapper::toResponse);
    }

    @Override
    @Transactional
    public LocationResponseDto update(UUID id, LocationRequestDto request) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> LocationException.notFound(id.toString()));

        Floor floor = resolveFloor(request);

        if (locationRepository.existsByDescriptionIgnoreCaseAndFloorIdAndIdNot(
                request.getDescription(), floor.getId(), id)) {

            throw LocationException.duplicateDescription(request.getDescription());
        }

        locationMapper.updateEntityFromRequest(request, location);
        location.setFloor(floor);

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> LocationException.notFound(id.toString()));

        if (assetRepository.existsByLocationId(id)) {
            throw LocationException.inUse(location.getDescription());
        }

        locationRepository.delete(location);
    }

    private Floor resolveFloor(LocationRequestDto request) {

        if (!campusRepository.existsById(request.getCampusId())) {
            throw CampusException.notFound(request.getCampusId().toString());
        }

        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> BuildingException.notFound(request.getBuildingId().toString()));

        if (!building.getCampus().getId().equals(request.getCampusId())) {
            throw BuildingException.campusDoesNotMatch(request.getBuildingId(), request.getCampusId());
        }

        String floorName = String.valueOf(request.getFloorNumber());

        return floorRepository.findByNameAndBuildingId(floorName, building.getId())
                .orElseGet(() -> {
                    Floor newFloor = Floor.builder()
                            .name(floorName)
                            .building(building)
                            .build();
                    return floorRepository.save(newFloor);
                });
    }
}
