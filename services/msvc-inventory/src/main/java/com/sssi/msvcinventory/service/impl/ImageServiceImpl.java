package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.ImageRequestDto;
import com.sssi.msvcinventory.dto.response.ImageResponseDto;
import com.sssi.msvcinventory.entity.Asset;
import com.sssi.msvcinventory.entity.Image;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.ImageException;
import com.sssi.msvcinventory.mapper.ImageMapper;
import com.sssi.msvcinventory.repository.ImageRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final AssetRepository assetRepository;
    private final ImageMapper imageMapper;

    @Override
    @Transactional
    public ImageResponseDto create(ImageRequestDto request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> AssetException.notFound(request.getAssetId().toString()));

        Image image = imageMapper.toEntity(request);
        image.setAsset(asset);

        return imageMapper.toResponse(imageRepository.save(image));
    }

    @Override
    @Transactional(readOnly = true)
    public ImageResponseDto findById(UUID id) {
        return imageRepository.findById(id)
                .map(imageMapper::toResponse)
                .orElseThrow(() -> ImageException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImageResponseDto> findByAssetId(UUID assetId, Pageable pageable) {
        if (!assetRepository.existsById(assetId)) {
            throw AssetException.notFound(assetId.toString());
        }
        return imageRepository.findByAssetId(assetId, pageable)
                .map(imageMapper::toResponse);
    }

    @Override
    @Transactional
    public ImageResponseDto update(UUID id, ImageRequestDto request) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> ImageException.notFound(id.toString()));

        imageMapper.updateEntityFromRequest(request, image);
        return imageMapper.toResponse(imageRepository.save(image));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> ImageException.notFound(id.toString()));
        imageRepository.delete(image);
    }
}
