package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetModelRequestDto;
import com.sssi.msvcinventory.dto.response.AssetModelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AssetModelService {

    AssetModelResponseDto create(AssetModelRequestDto request);

    AssetModelResponseDto findById(UUID id);

    Page<AssetModelResponseDto> findAll(Pageable pageable);

    Page<AssetModelResponseDto> findByBrandId(UUID brandId, Pageable pageable);

    Page<AssetModelResponseDto> findByAssetTypeId(UUID assetTypeId, Pageable pageable);

    AssetModelResponseDto update(UUID id, AssetModelRequestDto request);

    void delete(UUID id);
}
