package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.ExecutingUnitRequestDto;
import com.proseg.msvcinventory.dto.response.ExecutingUnitResponseDto;
import com.proseg.msvcinventory.entity.ExecutingUnit;
import com.proseg.msvcinventory.exception.ExecutingUnitException;
import com.proseg.msvcinventory.mapper.ExecutingUnitMapper;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.repository.ExecutingUnitRepository;
import com.proseg.msvcinventory.service.ExecutingUnitService;
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
