package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.AssetComponentRequestDto;
import com.proseg.msvcinventory.dto.response.AssetComponentResponseDto;
import com.proseg.msvcinventory.entity.Asset;
import com.proseg.msvcinventory.entity.AssetComponent;
import com.proseg.msvcinventory.exception.AssetComponentException;
import com.proseg.msvcinventory.exception.AssetException;
import com.proseg.msvcinventory.mapper.AssetComponentMapper;
import com.proseg.msvcinventory.repository.AssetComponentRepository;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.service.AssetComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetComponentServiceImpl implements AssetComponentService {

    private final AssetComponentRepository assetComponentRepository;
    private final AssetRepository assetRepository;
    private final AssetComponentMapper assetComponentMapper;

    @Override
    @Transactional
    public AssetComponentResponseDto create(UUID assetId, AssetComponentRequestDto request) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> AssetException.notFound(assetId.toString()));

        AssetComponent component = assetComponentMapper.toEntity(request);
        component.setAsset(asset);

        return assetComponentMapper.toResponse(assetComponentRepository.save(component));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetComponentResponseDto> findByAssetId(UUID assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw AssetException.notFound(assetId.toString());
        }

        return assetComponentRepository.findByAssetId(assetId).stream()
                .map(assetComponentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AssetComponentResponseDto update(UUID id, AssetComponentRequestDto request) {
        AssetComponent component = assetComponentRepository.findById(id)
                .orElseThrow(() -> AssetComponentException.notFound(id.toString()));

        assetComponentMapper.updateEntityFromRequest(request, component);

        return assetComponentMapper.toResponse(assetComponentRepository.save(component));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AssetComponent component = assetComponentRepository.findById(id)
                .orElseThrow(() -> AssetComponentException.notFound(id.toString()));
        assetComponentRepository.delete(component);
    }
}

