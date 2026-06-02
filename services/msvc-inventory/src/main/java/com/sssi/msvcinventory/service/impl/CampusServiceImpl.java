package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.CampusRequestDto;
import com.sssi.msvcinventory.dto.response.CampusResponseDto;
import com.sssi.msvcinventory.entity.Campus;
import com.sssi.msvcinventory.exception.CampusException;
import com.sssi.msvcinventory.mapper.CampusMapper;
import com.sssi.msvcinventory.repository.BuildingRepository;
import com.sssi.msvcinventory.repository.CampusRepository;
import com.sssi.msvcinventory.service.CampusService;
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
public class CampusServiceImpl implements CampusService {

    private final CampusRepository campusRepository;
    private final BuildingRepository buildingRepository;
    private final CampusMapper campusMapper;

    @Override
    @Transactional
    public CampusResponseDto create(CampusRequestDto request) {

        if (campusRepository.existsByNameIgnoreCase(request.getName())) {
            throw CampusException.duplicateName(request.getName());
        }

        Campus campus = campusMapper.toEntity(request);
        return campusMapper.toResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional(readOnly = true)
    public CampusResponseDto findById(UUID id) {
        return campusRepository.findById(id)
                .map(campusMapper::toResponse)
                .orElseThrow(() -> CampusException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampusResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Campus> spec = Specification
                .where(GenericSpecifications.<Campus>withSearch(Campus.class, search))
                .and(GenericSpecifications.<Campus>withColumnFilters(Campus.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Campus.class, pageable.getSort())
        );

        return campusRepository.findAll(spec, sanitized).map(campusMapper::toResponse);
    }

    @Override
    @Transactional
    public CampusResponseDto update(UUID id, CampusRequestDto request) {

        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> CampusException.notFound(id.toString()));

        if (campusRepository.existsByNameIgnoreCaseAndIdNot(
                request.getName(), id)) {

            throw CampusException.duplicateName(request.getName());
        }

        campusMapper.updateEntityFromRequest(request, campus);
        return campusMapper.toResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> CampusException.notFound(id.toString()));

        if (buildingRepository.existsByCampusId(id)) {
            throw CampusException.inUse(campus.getName());
        }

        campusRepository.delete(campus);
    }
}
