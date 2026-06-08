package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface AssetService {

    AssetResponseDto create(AssetRequestDto request);

    AssetResponseDto findById(UUID id);

    Page<AssetResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable);

    Page<AssetResponseDto> findByLocationId(UUID locationId, Pageable pageable);

    Page<AssetResponseDto> findByCampusId(UUID campusId, String search, Map<String, String> filters, Pageable pageable);

    Page<AssetResponseDto> findByBuildingId(UUID buildingId, String search, Map<String, String> filters, Pageable pageable);

    Page<AssetResponseDto> findByTypeId(UUID typeId, Pageable pageable);

    AssetResponseDto update(UUID id, AssetRequestDto request);

    void delete(UUID id);

    NetworkInterfaceResponseDto findLastKnownNetworkInterface(UUID assetId);

    boolean existsByAssetNumber(String assetNumber, UUID excludeId);
}
