package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetArchiveRequestDto;
import com.sssi.msvcinventory.dto.response.AssetArchiveResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AssetArchiveService {

    AssetArchiveResponseDto create(AssetArchiveRequestDto request);

    AssetArchiveResponseDto findById(UUID id);

    Page<AssetArchiveResponseDto> findByAssetId(UUID assetId, Pageable pageable);

    List<AssetArchiveResponseDto> getAssetArchivesByAssetId(UUID assetId);

    AssetArchiveResponseDto update(UUID id, AssetArchiveRequestDto request);

    void delete(UUID id);
}
