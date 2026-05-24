package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.LocationRequestDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import com.sssi.msvcinventory.entity.Location;
import com.sssi.msvcinventory.entity.Site;
import com.sssi.msvcinventory.exception.LocationException;
import com.sssi.msvcinventory.exception.SiteException;
import com.sssi.msvcinventory.mapper.LocationMapper;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.repository.SiteRepository;
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
    private final SiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public LocationResponseDto create(LocationRequestDto request) {

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> SiteException.notFound(request.getSiteId().toString()));

        if (locationRepository.existsByNameIgnoreCaseAndSiteId(
                request.getName(), request.getSiteId())) {

            throw LocationException.duplicateName(request.getName());
        }

        Location location = locationMapper.toEntity(request);
        location.setSite(site);

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
    public Page<LocationResponseDto> findBySiteId(UUID siteId, Pageable pageable) {

        if (!siteRepository.existsById(siteId)) {
            throw SiteException.notFound(siteId.toString());
        }

        return locationRepository.findBySiteId(siteId, pageable)
                .map(locationMapper::toResponse);
    }

    @Override
    @Transactional
    public LocationResponseDto update(UUID id, LocationRequestDto request) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> LocationException.notFound(id.toString()));

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> SiteException.notFound(request.getSiteId().toString()));

        if (locationRepository.existsByNameIgnoreCaseAndSiteIdAndIdNot(
                request.getName(), request.getSiteId(), id)) {

            throw LocationException.duplicateName(request.getName());
        }

        locationMapper.updateEntityFromRequest(request, location);
        location.setSite(site);

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> LocationException.notFound(id.toString()));

        if (assetRepository.existsByLocationId(id)) {
            throw LocationException.inUse(location.getName());
        }

        locationRepository.delete(location);
    }
}