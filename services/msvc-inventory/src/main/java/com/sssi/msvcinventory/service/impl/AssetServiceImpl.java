package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.request.NetworkInterfaceEmbeddedRequestDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.entity.*;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.AssetModelException;
import com.sssi.msvcinventory.exception.AssetTypeException;
import com.sssi.msvcinventory.exception.LocationException;
import com.sssi.msvcinventory.exception.NetworkInterfaceException;
import com.sssi.msvcinventory.mapper.*;
import com.sssi.msvcinventory.repository.AssetModelRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.AssetTypeRepository;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.repository.NetworkInterfaceRepository;
import com.sssi.msvcinventory.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetModelRepository assetModelRepository;
    private final LocationRepository locationRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final AssetMapper assetMapper;
    private final AlarmSensorMapper alarmSensorMapper;
    private final NetworkInterfaceMapper networkInterfaceMapper;

    @Override
    @Transactional
    public AssetResponseDto create(AssetRequestDto request) {
        AssetModel assetModel = assetModelRepository.findById(request.getAssetModelId())
                .orElseThrow(() -> AssetModelException.notFound(request.getAssetModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        AssetType assetType = assetModel.getAssetType();
        if (assetType.isRequiresNetworkInterface() && request.getNetworkInterface() == null) {
            throw AssetException.networkInterfaceRequired(assetType.getName());
        }

        Asset asset = assetMapper.toEntity(request);
        asset.setAssetModel(assetModel);
        asset.setLocation(location);
        Asset saved = assetRepository.save(asset);

        if (request.getNetworkInterface() != null) {
            saveNetworkInterface(request.getNetworkInterface(), saved);
        }

        return assetMapper.toResponse(assetRepository.findById(saved.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDto findById(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> AssetException.notFound(id.toString()));
        return toPolymorphicResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findAll(Pageable pageable) {
        return assetRepository.findAll(pageable)
                .map(this::toPolymorphicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByLocationId(UUID locationId, Pageable pageable) {
        if (!locationRepository.existsById(locationId)) {
            throw LocationException.notFound(locationId.toString());
        }
        return assetRepository.findByLocationId(locationId, pageable)
                .map(this::toPolymorphicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findBySiteId(UUID siteId, Pageable pageable) {
        return assetRepository.findByLocationSiteId(siteId, pageable)
                .map(this::toPolymorphicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByAssetTypeId(UUID assetTypeId, Pageable pageable) {
        if (!assetTypeRepository.existsById(assetTypeId)) {
            throw AssetTypeException.notFound(assetTypeId.toString());
        }
        return assetRepository.findByAssetModelAssetTypeId(assetTypeId, pageable)
                .map(this::toPolymorphicResponse);
    }

    @Override
    @Transactional
    public AssetResponseDto update(UUID id, AssetRequestDto request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> AssetException.notFound(id.toString()));

        AssetModel assetModel = assetModelRepository.findById(request.getAssetModelId())
                .orElseThrow(() -> AssetModelException.notFound(request.getAssetModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        AssetType assetType = assetModel.getAssetType();
        boolean hasExistingNi = networkInterfaceRepository.existsByAssetId(id);
        if (assetType.isRequiresNetworkInterface() && request.getNetworkInterface() == null && !hasExistingNi) {
            throw AssetException.networkInterfaceRequired(assetType.getName());
        }

        assetMapper.updateEntityFromRequest(request, asset);
        asset.setAssetModel(assetModel);
        asset.setLocation(location);
        assetRepository.save(asset);

        if (request.getNetworkInterface() != null) {
            networkInterfaceRepository.findByAssetId(id).ifPresentOrElse(
                    existing -> updateNetworkInterface(request.getNetworkInterface(), existing),
                    () -> saveNetworkInterface(request.getNetworkInterface(), asset)
            );
        }

        return toPolymorphicResponse(assetRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> AssetException.notFound(id.toString()));
        assetRepository.delete(asset);
    }

    private void saveNetworkInterface(NetworkInterfaceEmbeddedRequestDto dto, Asset asset) {
        if (networkInterfaceRepository.existsByIpAddress(dto.getIpAddress())) {
            throw NetworkInterfaceException.duplicateIp(dto.getIpAddress());
        }
        if (networkInterfaceRepository.existsByMacAddress(dto.getMacAddress())) {
            throw NetworkInterfaceException.duplicateMac(dto.getMacAddress());
        }
        NetworkInterface ni = networkInterfaceMapper.fromEmbedded(dto);
        ni.setAsset(asset);
        networkInterfaceRepository.save(ni);
    }

    private void updateNetworkInterface(NetworkInterfaceEmbeddedRequestDto dto, NetworkInterface existing) {
        if (networkInterfaceRepository.existsByIpAddressAndIdNot(dto.getIpAddress(), existing.getId())) {
            throw NetworkInterfaceException.duplicateIp(dto.getIpAddress());
        }
        if (networkInterfaceRepository.existsByMacAddressAndIdNot(dto.getMacAddress(), existing.getId())) {
            throw NetworkInterfaceException.duplicateMac(dto.getMacAddress());
        }
        existing.setIpAddress(dto.getIpAddress());
        existing.setMacAddress(dto.getMacAddress());
        networkInterfaceRepository.save(existing);
    }

    private AssetResponseDto toPolymorphicResponse(Asset asset) {
        if (asset instanceof AlarmSensor alarmSensor) {
            return alarmSensorMapper.toResponse(alarmSensor);
        }
        return assetMapper.toResponse(asset);
    }
}