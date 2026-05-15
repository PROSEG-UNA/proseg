package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.SiteRequestDto;
import com.sssi.msvcinventory.dto.response.SiteResponseDto;
import com.sssi.msvcinventory.entity.Site;
import com.sssi.msvcinventory.exception.SiteException;
import com.sssi.msvcinventory.mapper.SiteMapper;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.repository.SiteRepository;
import com.sssi.msvcinventory.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;
    private final SiteMapper siteMapper;

    @Override
    @Transactional
    public SiteResponseDto create(SiteRequestDto request) {

        if (siteRepository.existsByNameIgnoreCase(request.getName())) {
            throw SiteException.duplicateName(request.getName());
        }

        Site site = siteMapper.toEntity(request);
        return siteMapper.toResponse(siteRepository.save(site));
    }

    @Override
    @Transactional(readOnly = true)
    public SiteResponseDto findById(UUID id) {
        return siteRepository.findById(id)
                .map(siteMapper::toResponse)
                .orElseThrow(() -> SiteException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SiteResponseDto> findAll(Pageable pageable) {
        return siteRepository.findAll(pageable)
                .map(siteMapper::toResponse);
    }

    @Override
    @Transactional
    public SiteResponseDto update(UUID id, SiteRequestDto request) {

        Site site = siteRepository.findById(id)
                .orElseThrow(() -> SiteException.notFound(id.toString()));

        if (siteRepository.existsByNameIgnoreCaseAndIdNot(
                request.getName(), id)) {

            throw SiteException.duplicateName(request.getName());
        }

        siteMapper.updateEntityFromRequest(request, site);
        return siteMapper.toResponse(siteRepository.save(site));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Site site = siteRepository.findById(id)
                .orElseThrow(() -> SiteException.notFound(id.toString()));

        if (locationRepository.existsBySiteId(id)) {
            throw SiteException.inUse(site.getName());
        }

        siteRepository.delete(site);
    }
}