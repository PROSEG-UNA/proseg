package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.request.NetworkInterfaceEmbeddedRequestDto;
import com.sssi.msvcinventory.dto.response.AssetResponseDto;
import com.sssi.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.sssi.msvcinventory.entity.*;
import com.sssi.msvcinventory.entity.enums.AssetStatus;
import com.sssi.msvcinventory.exception.AssetException;
import com.sssi.msvcinventory.exception.ModelException;
import com.sssi.msvcinventory.exception.TypeException;
import com.sssi.msvcinventory.exception.LocationException;
import com.sssi.msvcinventory.exception.NetworkInterfaceException;
import com.sssi.msvcinventory.mapper.*;
import com.sssi.msvcinventory.repository.ModelRepository;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.specification.GenericSpecifications;
import com.sssi.msvcinventory.repository.TypeRepository;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.repository.NetworkInterfaceRepository;
import com.sssi.msvcinventory.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final ModelRepository modelRepository;
    private final LocationRepository locationRepository;
    private final TypeRepository typeRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final AssetMapper assetMapper;
    private final AlarmSensorMapper alarmSensorMapper;
    private final NetworkInterfaceMapper networkInterfaceMapper;

    @Override
    @Transactional
    public AssetResponseDto create(AssetRequestDto request) {
        if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()
                && assetRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw AssetException.duplicateSerialNumber(request.getSerialNumber());
        }

        validateDecommissionDate(request);

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> ModelException.notFound(request.getModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        Asset asset = assetMapper.toEntity(request);
        asset.setModel(model);
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
    public Page<AssetResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Asset> spec = Specification
                .where(GenericSpecifications.<Asset>withSearch(Asset.class, search))
                .and(GenericSpecifications.<Asset>withColumnFilters(Asset.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Asset.class, pageable.getSort())
        );

        return assetRepository.findAll(spec, sanitized).map(this::toPolymorphicResponse);
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
    public Page<AssetResponseDto> findByCampusId(UUID campusId, Pageable pageable) {
        return assetRepository.findByLocationFloorBuildingCampusId(campusId, pageable)
                .map(this::toPolymorphicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByTypeId(UUID typeId, Pageable pageable) {
        if (!typeRepository.existsById(typeId)) {
            throw TypeException.notFound(typeId.toString());
        }
        return assetRepository.findByModelTypeId(typeId, pageable)
                .map(this::toPolymorphicResponse);
    }

    @Override
    @Transactional
    public AssetResponseDto update(UUID id, AssetRequestDto request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> AssetException.notFound(id.toString()));

        if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()
                && assetRepository.existsBySerialNumberAndIdNot(request.getSerialNumber(), id)) {
            throw AssetException.duplicateSerialNumber(request.getSerialNumber());
        }

        validateDecommissionDate(request);

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> ModelException.notFound(request.getModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        Type type = model.getType();
        boolean hasExistingNi = networkInterfaceRepository.existsByAssetId(id);

        assetMapper.updateEntityFromRequest(request, asset);
        asset.setModel(model);
        asset.setLocation(location);
        assetRepository.save(asset);

        if (!type.isRequiresNetworkInterface()) {
            if (hasExistingNi) {
                NetworkInterface ni = asset.getNetworkInterface();
                asset.setNetworkInterface(null);
                assetRepository.saveAndFlush(asset);
                if (ni != null) {
                    networkInterfaceRepository.delete(ni);
                }
            }
        } else if (request.getNetworkInterface() != null) {
            Optional<NetworkInterface> active = networkInterfaceRepository.findByAssetId(id);
            if (active.isPresent()) {
                updateNetworkInterface(request.getNetworkInterface(), active.get());
            } else {
                networkInterfaceRepository.findByAssetIdIncludingDeleted(id).ifPresentOrElse(
                        deleted -> resurrectNetworkInterface(request.getNetworkInterface(), deleted),
                        () -> saveNetworkInterface(request.getNetworkInterface(), asset)
                );
            }
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

    @Override
    @Transactional(readOnly = true)
    public NetworkInterfaceResponseDto findLastKnownNetworkInterface(UUID assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw AssetException.notFound(assetId.toString());
        }
        return networkInterfaceRepository.findByAssetIdIncludingDeleted(assetId)
                .map(networkInterfaceMapper::toResponse)
                .orElseThrow(() -> NetworkInterfaceException.notFound(assetId.toString()));
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

    private void resurrectNetworkInterface(NetworkInterfaceEmbeddedRequestDto dto, NetworkInterface existing) {
        if (networkInterfaceRepository.existsByIpAddressAndIdNot(dto.getIpAddress(), existing.getId())) {
            throw NetworkInterfaceException.duplicateIp(dto.getIpAddress());
        }
        if (networkInterfaceRepository.existsByMacAddressAndIdNot(dto.getMacAddress(), existing.getId())) {
            throw NetworkInterfaceException.duplicateMac(dto.getMacAddress());
        }
        existing.setIpAddress(dto.getIpAddress());
        existing.setMacAddress(dto.getMacAddress());
        existing.markAsActive();
        networkInterfaceRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAssetNumber(String assetNumber, UUID excludeId) {
        if (excludeId == null) return assetRepository.existsByAssetNumber(assetNumber);
        return assetRepository.existsByAssetNumberAndIdNot(assetNumber, excludeId);
    }

    private void validateDecommissionDate(AssetRequestDto request) {
        if (request.getStatus() == AssetStatus.APROBADO && request.getDecommissionDate() != null) {
            throw AssetException.decommissionDateNotAllowed();
        }
    }

    private AssetResponseDto toPolymorphicResponse(Asset asset) {
        if (asset instanceof AlarmSensor alarmSensor) {
            return alarmSensorMapper.toResponse(alarmSensor);
        }
        return assetMapper.toResponse(asset);
    }
}