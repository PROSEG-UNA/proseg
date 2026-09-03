package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.TypeRequestDto;
import com.proseg.msvcinventory.dto.response.TypeResponseDto;
import com.proseg.msvcinventory.entity.Type;
import com.proseg.msvcinventory.exception.TypeException;
import com.proseg.msvcinventory.mapper.TypeMapper;
import com.proseg.msvcinventory.repository.ModelRepository;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.repository.NetworkInterfaceRepository;
import com.proseg.msvcinventory.repository.TypeRepository;
import com.proseg.msvcinventory.service.TypeService;
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
public class TypeServiceImpl implements TypeService {

    private final TypeRepository typeRepository;
    private final ModelRepository modelRepository;
    private final AssetRepository assetRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final TypeMapper typeMapper;

    @Override
    @Transactional
    public TypeResponseDto create(TypeRequestDto request) {
        if (typeRepository.existsByNameIgnoreCase(request.getName())) {
            throw TypeException.duplicateName(request.getName());
        }
        Type type = typeMapper.toEntity(request);
        return typeMapper.toResponse(typeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    public TypeResponseDto findById(UUID id) {
        return typeRepository.findById(id)
                .map(typeMapper::toResponse)
                .orElseThrow(() -> TypeException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TypeResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Type> spec = Specification
                .where(GenericSpecifications.<Type>withSearch(Type.class, search))
                .and(GenericSpecifications.<Type>withColumnFilters(Type.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Type.class, pageable.getSort())
        );

        return typeRepository.findAll(spec, sanitized).map(typeMapper::toResponse);
    }

    @Override
    @Transactional
    public TypeResponseDto update(UUID id, TypeRequestDto request) {
        Type type = typeRepository.findById(id)
                .orElseThrow(() -> TypeException.notFound(id.toString()));

        if (typeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw TypeException.duplicateName(request.getName());
        }

        boolean wasRequiringNi = type.isRequiresNetworkInterface();
        typeMapper.updateEntityFromRequest(request, type);
        typeRepository.save(type);

        if (wasRequiringNi && !type.isRequiresNetworkInterface()) {
            networkInterfaceRepository.softDeleteAllByAssetTypeId(id);
        }

        return typeMapper.toResponse(type);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Type type = typeRepository.findById(id)
                .orElseThrow(() -> TypeException.notFound(id.toString()));

        if (modelRepository.existsByTypeId(id) || assetRepository.existsByModelTypeId(id)) {
            throw TypeException.inUse(type.getName());
        }

        typeRepository.delete(type);
    }
}
