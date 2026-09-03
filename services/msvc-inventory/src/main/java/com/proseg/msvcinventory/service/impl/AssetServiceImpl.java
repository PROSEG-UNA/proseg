package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.AssetRequestDto;
import com.proseg.msvcinventory.dto.request.NetworkInterfaceEmbeddedRequestDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.dto.response.NetworkInterfaceResponseDto;
import com.proseg.msvcinventory.entity.*;
import com.proseg.msvcinventory.entity.enums.AssetStatus;
import com.proseg.msvcinventory.exception.AssetException;
import com.proseg.msvcinventory.exception.ModelException;
import com.proseg.msvcinventory.exception.TypeException;
import com.proseg.msvcinventory.exception.LocationException;
import com.proseg.msvcinventory.exception.NetworkInterfaceException;
import com.proseg.msvcinventory.exception.ExecutingUnitException;
import com.proseg.msvcinventory.exception.EmployeeException;
import com.proseg.msvcinventory.mapper.*;
import com.proseg.msvcinventory.repository.ModelRepository;
import com.proseg.msvcinventory.repository.ExecutingUnitRepository;
import com.proseg.msvcinventory.repository.EmployeeRepository;
import com.proseg.msvcinventory.repository.AssetRepository;
import com.proseg.msvcinventory.repository.AssetArchiveRepository;
import com.proseg.msvcinventory.repository.AssetComponentRepository;
import com.proseg.msvcinventory.repository.projection.AssetCountProjection;
import com.proseg.msvcinventory.specification.GenericSpecifications;
import com.proseg.msvcinventory.repository.TypeRepository;
import com.proseg.msvcinventory.repository.LocationRepository;
import com.proseg.msvcinventory.repository.NetworkInterfaceRepository;
import com.proseg.msvcinventory.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final ModelRepository modelRepository;
    private final LocationRepository locationRepository;
    private final TypeRepository typeRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final AssetArchiveRepository assetArchiveRepository;
    private final AssetComponentRepository assetComponentRepository;
    private final ExecutingUnitRepository executingUnitRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetMapper assetMapper;
    private final AlarmSensorMapper alarmSensorMapper;
    private final NetworkInterfaceMapper networkInterfaceMapper;

    @Override
    @Transactional
    public AssetResponseDto create(AssetRequestDto request) {
        request.setAssetNumber(trimToNull(request.getAssetNumber()));

        request.setSerialNumber(trimToNull(request.getSerialNumber()));
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
        asset.setExecutingUnit(resolveExecutingUnit(request.getExecutingUnitId()));
        asset.setEmployee(resolveEmployee(request.getEmployeeId()));
        Asset saved = assetRepository.save(asset);

        if (hasNetworkData(request.getNetworkInterface())) {
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

        return enrichWithDetailCounts(assetRepository.findAll(spec, sanitized).map(this::toPolymorphicResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByLocationId(UUID locationId, Pageable pageable) {
        if (!locationRepository.existsById(locationId)) {
            throw LocationException.notFound(locationId.toString());
        }
        return enrichWithDetailCounts(assetRepository.findByLocationId(locationId, pageable)
                .map(this::toPolymorphicResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByCampusId(UUID campusId, String search, Map<String, String> filters, Pageable pageable) {
        Specification<Asset> spec = Specification
                .where((Specification<Asset>) (root, query, cb) ->
                        cb.equal(root.get("location").get("floor").get("building").get("campus").get("id"), campusId))
                .and(GenericSpecifications.<Asset>withSearch(Asset.class, search))
                .and(GenericSpecifications.<Asset>withColumnFilters(Asset.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Asset.class, pageable.getSort())
        );

        return enrichWithDetailCounts(assetRepository.findAll(spec, sanitized).map(this::toPolymorphicResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByBuildingId(UUID buildingId, String search, Map<String, String> filters, Pageable pageable) {
        Specification<Asset> spec = Specification
                .where((Specification<Asset>) (root, query, cb) ->
                        cb.equal(root.get("location").get("floor").get("building").get("id"), buildingId))
                .and(GenericSpecifications.<Asset>withSearch(Asset.class, search))
                .and(GenericSpecifications.<Asset>withColumnFilters(Asset.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Asset.class, pageable.getSort())
        );

        return enrichWithDetailCounts(assetRepository.findAll(spec, sanitized).map(this::toPolymorphicResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDto> findByTypeId(UUID typeId, Pageable pageable) {
        if (!typeRepository.existsById(typeId)) {
            throw TypeException.notFound(typeId.toString());
        }
        return enrichWithDetailCounts(assetRepository.findByModelTypeId(typeId, pageable)
                .map(this::toPolymorphicResponse));
    }

    @Override
    @Transactional
    public AssetResponseDto update(UUID id, AssetRequestDto request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> AssetException.notFound(id.toString()));

        request.setAssetNumber(trimToNull(request.getAssetNumber()));

        request.setSerialNumber(trimToNull(request.getSerialNumber()));
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
        asset.setExecutingUnit(resolveExecutingUnit(request.getExecutingUnitId()));
        asset.setEmployee(resolveEmployee(request.getEmployeeId()));
        assetRepository.save(asset);

        if (!type.isRequiresNetworkInterface() || !hasNetworkData(request.getNetworkInterface())) {
            if (hasExistingNi) {
                NetworkInterface ni = asset.getNetworkInterface();
                asset.setNetworkInterface(null);
                assetRepository.saveAndFlush(asset);
                if (ni != null) {
                    networkInterfaceRepository.delete(ni);
                }
            }
        } else {
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

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExecutingUnit resolveExecutingUnit(UUID executingUnitId) {
        if (executingUnitId == null) return null;
        return executingUnitRepository.findById(executingUnitId)
                .orElseThrow(() -> ExecutingUnitException.notFound(executingUnitId.toString()));
    }

    private Employee resolveEmployee(UUID employeeId) {
        if (employeeId == null) return null;
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> EmployeeException.notFound(employeeId.toString()));
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
        String ip = normalizeBlankToNull(dto.getIpAddress());
        String mac = normalizeBlankToNull(dto.getMacAddress());
        if (ip != null && networkInterfaceRepository.existsByIpAddress(ip)) {
            throw NetworkInterfaceException.duplicateIp(ip);
        }
        if (mac != null && networkInterfaceRepository.existsByMacAddress(mac)) {
            throw NetworkInterfaceException.duplicateMac(mac);
        }
        NetworkInterface ni = networkInterfaceMapper.fromEmbedded(dto);
        ni.setIpAddress(ip);
        ni.setMacAddress(mac);
        ni.setAsset(asset);
        networkInterfaceRepository.save(ni);
    }

    private void updateNetworkInterface(NetworkInterfaceEmbeddedRequestDto dto, NetworkInterface existing) {
        String ip = normalizeBlankToNull(dto.getIpAddress());
        String mac = normalizeBlankToNull(dto.getMacAddress());
        if (ip != null && networkInterfaceRepository.existsByIpAddressAndIdNot(ip, existing.getId())) {
            throw NetworkInterfaceException.duplicateIp(ip);
        }
        if (mac != null && networkInterfaceRepository.existsByMacAddressAndIdNot(mac, existing.getId())) {
            throw NetworkInterfaceException.duplicateMac(mac);
        }
        existing.setIpAddress(ip);
        existing.setMacAddress(mac);
        networkInterfaceRepository.save(existing);
    }

    private void resurrectNetworkInterface(NetworkInterfaceEmbeddedRequestDto dto, NetworkInterface existing) {
        String ip = normalizeBlankToNull(dto.getIpAddress());
        String mac = normalizeBlankToNull(dto.getMacAddress());
        if (ip != null && networkInterfaceRepository.existsByIpAddressAndIdNot(ip, existing.getId())) {
            throw NetworkInterfaceException.duplicateIp(ip);
        }
        if (mac != null && networkInterfaceRepository.existsByMacAddressAndIdNot(mac, existing.getId())) {
            throw NetworkInterfaceException.duplicateMac(mac);
        }
        existing.setIpAddress(ip);
        existing.setMacAddress(mac);
        existing.markAsActive();
        networkInterfaceRepository.save(existing);
    }

    private boolean hasNetworkData(NetworkInterfaceEmbeddedRequestDto dto) {
        return dto != null
                && (normalizeBlankToNull(dto.getIpAddress()) != null
                || normalizeBlankToNull(dto.getMacAddress()) != null);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private Page<AssetResponseDto> enrichWithDetailCounts(Page<AssetResponseDto> page) {
        List<UUID> assetIds = page.getContent().stream()
                .map(AssetResponseDto::getId)
                .filter(Objects::nonNull)
                .toList();

        if (assetIds.isEmpty()) return page;

        Map<UUID, Long> imagesByAsset = toCountMap(assetArchiveRepository.countByAssetIds(assetIds));
        Map<UUID, Long> componentsByAsset = toCountMap(assetComponentRepository.countByAssetIds(assetIds));

        page.getContent().forEach(asset -> {
            asset.setImagesCount(imagesByAsset.getOrDefault(asset.getId(), 0L).intValue());
            asset.setComponentsCount(componentsByAsset.getOrDefault(asset.getId(), 0L).intValue());
        });

        return page;
    }

    private Map<UUID, Long> toCountMap(List<AssetCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(AssetCountProjection::getAssetId, AssetCountProjection::getTotal));
    }
}