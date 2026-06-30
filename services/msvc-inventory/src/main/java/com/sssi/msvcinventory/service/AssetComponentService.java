package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetComponentRequestDto;
import com.sssi.msvcinventory.dto.response.AssetComponentResponseDto;

import java.util.List;
import java.util.UUID;

public interface AssetComponentService {

    AssetComponentResponseDto create(UUID assetId, AssetComponentRequestDto request);

    List<AssetComponentResponseDto> findByAssetId(UUID assetId);

    AssetComponentResponseDto update(UUID id, AssetComponentRequestDto request);

    void delete(UUID id);
}

