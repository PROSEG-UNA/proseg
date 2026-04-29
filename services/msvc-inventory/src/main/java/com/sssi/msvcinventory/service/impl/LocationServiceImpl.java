package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.LocationRequestDto;
import com.sssi.msvcinventory.dto.response.LocationResponseDto;
import com.sssi.msvcinventory.entity.Location;
import com.sssi.msvcinventory.entity.Site;
import com.sssi.msvcinventory.exception.LocationException;
import com.sssi.msvcinventory.exception.SiteException;
import com.sssi.msvcinventory.mapper.LocationMapper;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.repository.SiteRepository;
import com.sssi.msvcinventory.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final SiteRepository siteRepository;
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
    public Page<LocationResponseDto> findAll(Pageable pageable) {
        return locationRepository.findAll(pageable)
                .map(locationMapper::toResponse);
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

        locationRepository.delete(location);
    }
}