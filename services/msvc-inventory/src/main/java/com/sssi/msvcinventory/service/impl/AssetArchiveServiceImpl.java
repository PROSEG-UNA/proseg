package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetArchiveRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;
import com.sssi.msvcinventory.entity.Asset;
import com.sssi.msvcinventory.entity.AssetArchive;
import com.sssi.msvcinventory.exception.AssetArchiveException;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.mapper.AssetArchiveMapper;
import com.sssi.msvcinventory.repository.AssetArchiveRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.service.AssetArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetArchiveServiceImpl implements AssetArchiveService {

    private final AssetArchiveRepository assetArchiveRepository;
    private final AssetRepository assetRepository;
    private final AssetArchiveMapper assetArchiveMapper;

    @Override
    @Transactional
    public AssetArchiveResponseDto create(AssetArchiveRequestDto request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> AssetException.notFound(request.getAssetId().toString()));

        AssetArchive assetArchive = assetArchiveMapper.toEntity(request);
        assetArchive.setAsset(asset);

        return toResponse(assetArchiveRepository.save(assetArchive));
    }

    @Override
    @Transactional(readOnly = true)
    public AssetArchiveResponseDto findById(UUID id) {
        return assetArchiveRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> AssetArchiveException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetArchiveResponseDto> findByAssetId(UUID assetId, Pageable pageable) {
        if (!assetRepository.existsById(assetId)) {
            throw AssetException.notFound(assetId.toString());
        }
        return assetArchiveRepository.findByAssetId(assetId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetArchiveResponseDto> getAssetArchivesByAssetId(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> AssetException.notFound(assetId.toString()));

        return assetArchiveRepository.findByAssetId(asset.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssetArchiveResponseDto update(UUID id, AssetArchiveRequestDto request) {
        AssetArchive assetArchive = assetArchiveRepository.findById(id)
                .orElseThrow(() -> AssetArchiveException.notFound(id.toString()));

        assetArchiveMapper.updateEntityFromRequest(request, assetArchive);
        return toResponse(assetArchiveRepository.save(assetArchive));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AssetArchive assetArchive = assetArchiveRepository.findById(id)
                .orElseThrow(() -> AssetArchiveException.notFound(id.toString()));
        assetArchiveRepository.delete(assetArchive);
    }

    public String getPresignedGetUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return "/api/v1/archive/files/" + objectName;
    }

    private AssetArchiveResponseDto toResponse(AssetArchive assetArchive) {
        return AssetArchiveResponseDto.builder()
                .id(assetArchive.getId())
                .caption(assetArchive.getCaption())
                .imageUrl(getPresignedGetUrl(assetArchive.getObjectName()))
                .build();
    }
}
