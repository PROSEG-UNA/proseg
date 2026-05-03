package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetTypeRequestDto;
import com.sssi.msvcinventory.dto.response.AssetTypeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AssetTypeService {

    AssetTypeResponseDto create(AssetTypeRequestDto request);

    AssetTypeResponseDto findById(UUID id);

    Page<AssetTypeResponseDto> findAll(Pageable pageable);

    AssetTypeResponseDto update(UUID id, AssetTypeRequestDto request);

    void delete(UUID id);
}
