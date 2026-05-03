package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.NetworkInterfaceRequestDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.sssi.msvcinventory.entity.Asset;
import com.sssi.msvcinventory.entity.NetworkInterface;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.NetworkInterfaceException;
import com.sssi.msvcinventory.mapper.NetworkInterfaceMapper;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.NetworkInterfaceRepository;
import com.sssi.msvcinventory.service.NetworkInterfaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NetworkInterfaceServiceImpl implements NetworkInterfaceService {

    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final AssetRepository assetRepository;
    private final NetworkInterfaceMapper networkInterfaceMapper;

    @Override
    @Transactional
    public NetworkInterfaceResponseDto create(NetworkInterfaceRequestDto request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> AssetException.notFound(request.getAssetId().toString()));

        if (networkInterfaceRepository.existsByAssetId(request.getAssetId())) {
            throw NetworkInterfaceException.assetAlreadyHasInterface(request.getAssetId().toString());
        }
        if (networkInterfaceRepository.existsByIpAddress(request.getIpAddress())) {
            throw NetworkInterfaceException.duplicateIp(request.getIpAddress());
        }
        if (networkInterfaceRepository.existsByMacAddress(request.getMacAddress())) {
            throw NetworkInterfaceException.duplicateMac(request.getMacAddress());
        }

        NetworkInterface networkInterface = networkInterfaceMapper.toEntity(request);
        networkInterface.setAsset(asset);

        return networkInterfaceMapper.toResponse(networkInterfaceRepository.save(networkInterface));
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkInterfaceResponseDto findById(UUID id) {
        return networkInterfaceRepository.findById(id)
                .map(networkInterfaceMapper::toResponse)
                .orElseThrow(() -> NetworkInterfaceException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkInterfaceResponseDto findByAssetId(UUID assetId) {
        return networkInterfaceRepository.findByAssetId(assetId)
                .map(networkInterfaceMapper::toResponse)
                .orElseThrow(() -> NetworkInterfaceException.notFound(assetId.toString()));
    }

    @Override
    @Transactional
    public NetworkInterfaceResponseDto update(UUID id, NetworkInterfaceRequestDto request) {
        NetworkInterface networkInterface = networkInterfaceRepository.findById(id)
                .orElseThrow(() -> NetworkInterfaceException.notFound(id.toString()));

        if (networkInterfaceRepository.existsByIpAddressAndIdNot(request.getIpAddress(), id)) {
            throw NetworkInterfaceException.duplicateIp(request.getIpAddress());
        }
        if (networkInterfaceRepository.existsByMacAddressAndIdNot(request.getMacAddress(), id)) {
            throw NetworkInterfaceException.duplicateMac(request.getMacAddress());
        }

        networkInterfaceMapper.updateEntityFromRequest(request, networkInterface);

        return networkInterfaceMapper.toResponse(networkInterfaceRepository.save(networkInterface));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        NetworkInterface networkInterface = networkInterfaceRepository.findById(id)
                .orElseThrow(() -> NetworkInterfaceException.notFound(id.toString()));

        if (networkInterface.getAsset().getAssetModel().getAssetType().isRequiresNetworkInterface()) {
            throw NetworkInterfaceException.requiredByAssetType(
                    networkInterface.getAsset().getAssetModel().getAssetType().getName()
            );
        }

        networkInterfaceRepository.delete(networkInterface);
    }
}