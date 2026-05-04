package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.ImageRequestDto;
import com.sssi.msvcinventory.dto.response.ImageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ImageService {

    ImageResponseDto create(ImageRequestDto request);

    ImageResponseDto findById(UUID id);

    Page<ImageResponseDto> findByAssetId(UUID assetId, Pageable pageable);

    ImageResponseDto update(UUID id, ImageRequestDto request);

    void delete(UUID id);
}
