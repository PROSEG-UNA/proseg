package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetImageRequestDto;
import com.sssi.msvcinventory.dto.response.AssetImageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AssetImageService {

    AssetImageResponseDto create(AssetImageRequestDto request);

    AssetImageResponseDto findById(UUID id);

    Page<AssetImageResponseDto> findByAssetId(UUID assetId, Pageable pageable);

    AssetImageResponseDto update(UUID id, AssetImageRequestDto request);

    void delete(UUID id);
}
