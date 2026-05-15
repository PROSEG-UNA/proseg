package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.TypeRequestDto;
import com.sssi.msvcinventory.dto.response.TypeResponseDto;
import com.sssi.msvcinventory.entity.Type;
import com.sssi.msvcinventory.exception.TypeException;
import com.sssi.msvcinventory.mapper.TypeMapper;
import com.sssi.msvcinventory.repository.ModelRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.NetworkInterfaceRepository;
import com.sssi.msvcinventory.repository.TypeRepository;
import com.sssi.msvcinventory.service.TypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<TypeResponseDto> findAll(Pageable pageable) {
        return typeRepository.findAll(pageable)
                .map(typeMapper::toResponse);
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
