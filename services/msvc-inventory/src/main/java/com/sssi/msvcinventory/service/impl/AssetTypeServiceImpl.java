package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetTypeRequestDto;
import com.sssi.msvcinventory.dto.response.AssetTypeResponseDto;
import com.sssi.msvcinventory.entity.AssetType;
import com.sssi.msvcinventory.exception.AssetTypeException;
import com.sssi.msvcinventory.mapper.AssetTypeMapper;
import com.sssi.msvcinventory.repository.AssetModelRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.AssetTypeRepository;
import com.sssi.msvcinventory.service.AssetTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetTypeServiceImpl implements AssetTypeService {

    private final AssetTypeRepository assetTypeRepository;
    private final AssetModelRepository assetModelRepository;
    private final AssetRepository assetRepository;
    private final AssetTypeMapper assetTypeMapper;

    @Override
    @Transactional
    public AssetTypeResponseDto create(AssetTypeRequestDto request) {
        if (assetTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw AssetTypeException.duplicateName(request.getName());
        }
        AssetType assetType = assetTypeMapper.toEntity(request);
        return assetTypeMapper.toResponse(assetTypeRepository.save(assetType));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetTypeResponseDto findById(UUID id) {
        return assetTypeRepository.findById(id)
                .map(assetTypeMapper::toResponse)
                .orElseThrow(() -> AssetTypeException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetTypeResponseDto> findAll(Pageable pageable) {
        return assetTypeRepository.findAll(pageable)
                .map(assetTypeMapper::toResponse);
    }

    @Override
    @Transactional
    public AssetTypeResponseDto update(UUID id, AssetTypeRequestDto request) {
        AssetType assetType = assetTypeRepository.findById(id)
                .orElseThrow(() -> AssetTypeException.notFound(id.toString()));

        if (assetTypeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw AssetTypeException.duplicateName(request.getName());
        }

        assetTypeMapper.updateEntityFromRequest(request, assetType);
        return assetTypeMapper.toResponse(assetTypeRepository.save(assetType));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AssetType assetType = assetTypeRepository.findById(id)
                .orElseThrow(() -> AssetTypeException.notFound(id.toString()));

        if (assetModelRepository.existsByAssetTypeId(id) || assetRepository.existsByAssetModelAssetTypeId(id)) {
            throw AssetTypeException.inUse(id.toString());
        }

        assetTypeRepository.delete(assetType);
    }
}
