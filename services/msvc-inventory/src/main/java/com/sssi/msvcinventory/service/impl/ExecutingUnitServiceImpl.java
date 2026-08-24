package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.ExecutingUnitRequestDto;
import com.sssi.msvcinventory.dto.response.ExecutingUnitResponseDto;
import com.sssi.msvcinventory.entity.ExecutingUnit;
import com.sssi.msvcinventory.exception.ExecutingUnitException;
import com.sssi.msvcinventory.mapper.ExecutingUnitMapper;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.ExecutingUnitRepository;
import com.sssi.msvcinventory.service.ExecutingUnitService;
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
public class ExecutingUnitServiceImpl implements ExecutingUnitService {

    private final ExecutingUnitRepository executingUnitRepository;
    private final AssetRepository assetRepository;
    private final ExecutingUnitMapper executingUnitMapper;

    @Override
    @Transactional
    public ExecutingUnitResponseDto create(ExecutingUnitRequestDto request) {

        if (executingUnitRepository.existsByNameIgnoreCase(request.getName())) {
            throw ExecutingUnitException.duplicateName(request.getName());
        }

        ExecutingUnit executingUnit = executingUnitMapper.toEntity(request);
        return executingUnitMapper.toResponse(executingUnitRepository.save(executingUnit));
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutingUnitResponseDto findById(UUID id) {
        return executingUnitRepository.findById(id)
                .map(executingUnitMapper::toResponse)
                .orElseThrow(() -> ExecutingUnitException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExecutingUnitResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<ExecutingUnit> spec = Specification
                .where(GenericSpecifications.<ExecutingUnit>withSearch(ExecutingUnit.class, search))
                .and(GenericSpecifications.<ExecutingUnit>withColumnFilters(ExecutingUnit.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(ExecutingUnit.class, pageable.getSort())
        );

        return executingUnitRepository.findAll(spec, sanitized).map(executingUnitMapper::toResponse);
    }

    @Override
    @Transactional
    public ExecutingUnitResponseDto update(UUID id, ExecutingUnitRequestDto request) {

        ExecutingUnit executingUnit = executingUnitRepository.findById(id)
                .orElseThrow(() -> ExecutingUnitException.notFound(id.toString()));

        if (executingUnitRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw ExecutingUnitException.duplicateName(request.getName());
        }

        executingUnitMapper.updateEntityFromRequest(request, executingUnit);
        return executingUnitMapper.toResponse(executingUnitRepository.save(executingUnit));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        ExecutingUnit executingUnit = executingUnitRepository.findById(id)
                .orElseThrow(() -> ExecutingUnitException.notFound(id.toString()));

        if (assetRepository.existsByExecutingUnitId(id)) {
            throw ExecutingUnitException.inUse(executingUnit.getName());
        }

        executingUnitRepository.delete(executingUnit);
    }
}
