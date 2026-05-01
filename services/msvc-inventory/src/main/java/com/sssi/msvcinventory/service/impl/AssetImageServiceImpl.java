package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetImageRequestDto;
import com.sssi.msvcinventory.dto.response.AssetImageResponseDto;
import com.sssi.msvcinventory.entity.Asset;
import com.sssi.msvcinventory.entity.AssetImage;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.AssetImageException;
import com.sssi.msvcinventory.mapper.AssetImageMapper;
import com.sssi.msvcinventory.repository.AssetImageRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.service.AssetImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetImageServiceImpl implements AssetImageService {

    private final AssetImageRepository assetImageRepository;
    private final AssetRepository assetRepository;
    private final AssetImageMapper assetImageMapper;

    @Override
    @Transactional
    public AssetImageResponseDto create(AssetImageRequestDto request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> AssetException.notFound(request.getAssetId().toString()));

        AssetImage assetImage = assetImageMapper.toEntity(request);
        assetImage.setAsset(asset);

        return assetImageMapper.toResponse(assetImageRepository.save(assetImage));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetImageResponseDto findById(UUID id) {
        return assetImageRepository.findById(id)
                .map(assetImageMapper::toResponse)
                .orElseThrow(() -> AssetImageException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetImageResponseDto> findByAssetId(UUID assetId, Pageable pageable) {
        if (!assetRepository.existsById(assetId)) {
            throw AssetException.notFound(assetId.toString());
        }
        return assetImageRepository.findByAssetId(assetId, pageable)
                .map(assetImageMapper::toResponse);
    }

    @Override
    @Transactional
    public AssetImageResponseDto update(UUID id, AssetImageRequestDto request) {
        AssetImage assetImage = assetImageRepository.findById(id)
                .orElseThrow(() -> AssetImageException.notFound(id.toString()));

        assetImageMapper.updateEntityFromRequest(request, assetImage);
        return assetImageMapper.toResponse(assetImageRepository.save(assetImage));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AssetImage assetImage = assetImageRepository.findById(id)
                .orElseThrow(() -> AssetImageException.notFound(id.toString()));
        assetImageRepository.delete(assetImage);
    }
}
