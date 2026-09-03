package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.NetworkInterfaceRequestDto;
import com.proseg.msvcinventory.dto.response.NetworkInterfaceResponseDto;

import java.util.UUID;

public interface NetworkInterfaceService {

    NetworkInterfaceResponseDto create(NetworkInterfaceRequestDto request);

    NetworkInterfaceResponseDto findById(UUID id);

    NetworkInterfaceResponseDto findByAssetId(UUID assetId);

    NetworkInterfaceResponseDto update(UUID id, NetworkInterfaceRequestDto request);

    void delete(UUID id);
}