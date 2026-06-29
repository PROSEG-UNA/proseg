package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AssetImportRowDto;
import com.sssi.msvcinventory.dto.request.AssetRequestDto;
import com.sssi.msvcinventory.dto.response.PendingCreationDto;
import com.sssi.msvcinventory.entity.*;
import com.sssi.msvcinventory.entity.enums.AssetStatus;
import com.sssi.msvcinventory.exception.AssetImportException;
import com.sssi.msvcinventory.importer.AssetStatusResolver;
import com.sssi.msvcinventory.importer.ImportNameNormalizer;
import com.sssi.msvcinventory.repository.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetImportRowProcessor {

    private static final String DEFAULT_BUILDING_NAME = "-";
    private static final String DEFAULT_FLOOR_NAME = "1";
    private static final String DEFAULT_LOCATION_NAME = "-";

    private static final UUID VALIDATION_REFERENCE_PLACEHOLDER = new UUID(0L, 0L);
    private static final List<String> FIELD_VALIDATION_ORDER = List.of(
            "assetNumber", "serialNumber", "responsibleEmployee",
            "responsibleEmployeeId", "executingUnit", "latitude", "longitude");

    private final AssetRepository assetRepository;
    private final TypeRepository typeRepository;
    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;
    private final CampusRepository campusRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final LocationRepository locationRepository;
    private final NetworkInterfaceRepository networkInterfaceRepository;
    private final Validator validator;

    public void validateRow(AssetImportRowDto row,
                            Set<String> duplicateAssetNumbers,
                            Set<String> duplicateSerials,
                            Set<String> duplicateIpsInFile,
                            Set<String> duplicateMacsInFile) {
        requireNonBlank(row.getStatus(), "Estado");

        AssetStatus status = AssetStatusResolver.resolve(row.getStatus())
                .orElseThrow(AssetImportException::invalidStatus);

        validateModel(row);
        validateLocation(row);
        validateNetworkInterface(row, duplicateIpsInFile, duplicateMacsInFile);

        validateFieldRules(row, status);

        String assetNumber = row.getAssetNumber().trim();
        if (duplicateAssetNumbers.contains(assetNumber)) {
            throw AssetImportException.duplicateAssetNumberInFile();
        }
        if (assetRepository.existsByAssetNumber(assetNumber)) {
            throw AssetImportException.duplicateAssetNumber();
        }

        String serial = trimToNull(row.getSerialNumber());
        if (serial != null) {
            if (duplicateSerials.contains(serial)) {
                throw AssetImportException.duplicateSerialNumberInFile();
            }
            if (assetRepository.existsBySerialNumber(serial)) {
                throw AssetImportException.duplicateSerialNumber();
            }
        }
    }

    public void persistRows(List<AssetImportRowDto> rows) {
        LocationMatchCache cache = new LocationMatchCache();
        for (AssetImportRowDto row : rows) {
            persistRow(row, cache);
        }
    }

    private void persistRow(AssetImportRowDto row, LocationMatchCache cache) {
        AssetStatus status = AssetStatusResolver.resolve(row.getStatus())
                .orElseThrow(AssetImportException::invalidStatus);

        Model model = resolveModel(row);
        Location location = resolveLocation(row, cache);

        Asset asset = buildAsset(row, model, location, status);
        Asset saved = assetRepository.save(asset);

        resolveNetworkInterface(row, saved);
    }

    private void requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw AssetImportException.requiredField(fieldLabel);
        }
    }

    private void validateFieldRules(AssetImportRowDto row, AssetStatus status) {
        AssetRequestDto dto = buildValidationDto(row, status);
        Set<ConstraintViolation<AssetRequestDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw AssetImportException.fieldRule(firstViolationMessage(violations));
        }
    }

    private AssetRequestDto buildValidationDto(AssetImportRowDto row, AssetStatus status) {
        return AssetRequestDto.builder()
                .modelId(VALIDATION_REFERENCE_PLACEHOLDER)
                .locationId(VALIDATION_REFERENCE_PLACEHOLDER)
                .status(status)
                .assetNumber(trimToNull(row.getAssetNumber()))
                .serialNumber(trimToNull(row.getSerialNumber()))
                .executingUnit(trimToNull(row.getExecutingUnit()))
                .responsibleEmployee(trimToNull(row.getResponsibleEmployee()))
                .responsibleEmployeeId(trimToNull(row.getResponsibleEmployeeId()))
                .acquisitionDate(row.getAcquisitionDate())
                .warrantyEndDate(row.getWarrantyEndDate())
                .firmwareSupportEndDate(row.getFirmwareSupportEndDate())
                .latitude(row.getLatitude())
                .longitude(row.getLongitude())
                .build();
    }

    private String firstViolationMessage(Set<ConstraintViolation<AssetRequestDto>> violations) {
        return violations.stream()
                .min(Comparator.comparingInt(this::violationOrder))
                .map(ConstraintViolation::getMessage)
                .orElseThrow();
    }

    private int violationOrder(ConstraintViolation<AssetRequestDto> violation) {
        int index = FIELD_VALIDATION_ORDER.indexOf(violation.getPropertyPath().toString());
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public List<PendingCreationDto> planCreations(List<AssetImportRowDto> rows) {
        Set<String> planned = new LinkedHashSet<>();
        List<PendingCreationDto> pending = new ArrayList<>();
        LocationMatchCache cache = new LocationMatchCache();
        for (AssetImportRowDto row : rows) {
            planModelCreations(row, planned, pending);
            planLocationCreations(row, planned, pending, cache);
        }
        return pending;
    }

    private void planModelCreations(AssetImportRowDto row, Set<String> planned, List<PendingCreationDto> pending) {
        if (isBlank(row.getModelName()) || isBlank(row.getTypeName()) || isBlank(row.getBrandName())) {
            return;
        }
        String typeName = row.getTypeName().trim();
        String brandName = row.getBrandName().trim();
        String modelName = row.getModelName().trim();

        if (typeRepository.findFirstByNameIgnoreCase(typeName).isEmpty()) {
            addPending(planned, pending, key("TYPE", typeName),
                    "Tipo", "Se creará el tipo '" + typeName + "'");
        }

        Optional<Brand> brand = brandRepository.findFirstByNameIgnoreCase(brandName);
        if (brand.isEmpty()) {
            addPending(planned, pending, key("BRAND", brandName),
                    "Marca", "Se creará la marca '" + brandName + "'");
        }

        boolean modelExists = brand.isPresent()
                && modelRepository.findFirstByNameIgnoreCaseAndBrandId(modelName, brand.get().getId()).isPresent();
        if (!modelExists) {
            addPending(planned, pending, key("MODEL", brandName, modelName),
                    "Modelo", "Se creará el modelo '" + modelName + "' de la marca '" + brandName
                            + "' (tipo '" + typeName + "')");
        }
    }

    private void planLocationCreations(AssetImportRowDto row, Set<String> planned, List<PendingCreationDto> pending,
                                       LocationMatchCache cache) {
        if (isBlank(row.getCampusName())) {
            return;
        }
        String campusName = row.getCampusName().trim();
        Optional<Campus> campus = findMatchingCampus(campusName, cache);
        if (campus.isEmpty()) {
            addPending(planned, pending, key("CAMPUS", campusName),
                    "Campus", "Se creará el campus '" + campusName + "'");
        }

        if (isBlank(row.getBuildingName())) {
            return;
        }
        String buildingName = row.getBuildingName().trim();
        boolean buildingExists = campus.isPresent()
                && findMatchingBuilding(buildingName, campus.get(), cache).isPresent();
        if (!buildingExists) {
            addPending(planned, pending, key("BUILDING", campusName, buildingName),
                    "Edificio", "Se creará el edificio '" + buildingName + "' en el campus '" + campusName + "'");
        }
    }

    private void addPending(Set<String> planned, List<PendingCreationDto> pending, String key,
                            String label, String message) {
        if (planned.add(key)) {
            pending.add(PendingCreationDto.builder()
                    .key(key)
                    .label(label)
                    .message(message)
                    .build());
        }
    }

    private String key(String kind, String... parts) {
        StringBuilder sb = new StringBuilder(kind);
        for (String part : parts) {
            sb.append('|').append(part.toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private Model resolveModel(AssetImportRowDto row) {
        if (isBlank(row.getModelName())) {
            throw AssetImportException.modelRequired();
        }
        boolean hasType = !isBlank(row.getTypeName());
        boolean hasBrand = !isBlank(row.getBrandName());

        if (hasBrand && hasType) {
            return resolveModelCreating(row);
        }
        if (hasBrand) {
            return resolveModelFromBrand(row);
        }
        return resolveModelGlobally(row);
    }

    private Model resolveModelCreating(AssetImportRowDto row) {
        String modelName = row.getModelName().trim();
        Type type = findOrCreateType(row.getTypeName().trim(), hasNetworkInfo(row));
        Brand brand = findOrCreateBrand(row.getBrandName().trim());

        return modelRepository.findFirstByNameIgnoreCaseAndBrandId(modelName, brand.getId())
                .orElseGet(() -> modelRepository.save(Model.builder()
                        .name(modelName)
                        .brand(brand)
                        .type(type)
                        .build()));
    }

    private Model resolveModelFromBrand(AssetImportRowDto row) {
        Brand brand = brandRepository.findFirstByNameIgnoreCase(row.getBrandName().trim())
                .orElseThrow(AssetImportException::brandNotFound);
        return modelRepository.findFirstByNameIgnoreCaseAndBrandId(row.getModelName().trim(), brand.getId())
                .orElseThrow(AssetImportException::modelNotFound);
    }

    private Model resolveModelGlobally(AssetImportRowDto row) {
        List<Model> matches = findModelCandidates(row);
        if (matches.isEmpty()) {
            throw AssetImportException.modelNotFound();
        }
        if (matches.size() > 1) {
            throw AssetImportException.ambiguousModel();
        }
        return matches.get(0);
    }

    private List<Model> findModelCandidates(AssetImportRowDto row) {
        String modelName = row.getModelName().trim();
        if (!isBlank(row.getTypeName())) {
            return modelRepository.findByNameIgnoreCaseAndType_NameIgnoreCase(modelName, row.getTypeName().trim());
        }
        return modelRepository.findByNameIgnoreCase(modelName);
    }

    private Type findOrCreateType(String name, boolean requiresNetworkInterface) {
        return typeRepository.findFirstByNameIgnoreCase(name)
                .orElseGet(() -> typeRepository.save(Type.builder()
                        .name(name)
                        .requiresNetworkInterface(requiresNetworkInterface)
                        .build()));
    }

    private Brand findOrCreateBrand(String name) {
        return brandRepository.findFirstByNameIgnoreCase(name)
                .orElseGet(() -> brandRepository.save(Brand.builder().name(name).build()));
    }

    private boolean hasNetworkInfo(AssetImportRowDto row) {
        return !isBlank(row.getIpAddress()) || !isBlank(row.getMacAddress());
    }

    private void validateModel(AssetImportRowDto row) {
        if (isBlank(row.getModelName())) {
            throw AssetImportException.modelRequired();
        }
        boolean hasType = !isBlank(row.getTypeName());
        boolean hasBrand = !isBlank(row.getBrandName());

        if (hasBrand && hasType) {
            validateModelType(row);
            return;
        }
        if (hasBrand) {
            Brand brand = brandRepository.findFirstByNameIgnoreCase(row.getBrandName().trim())
                    .orElseThrow(AssetImportException::brandNotFound);
            modelRepository.findFirstByNameIgnoreCaseAndBrandId(row.getModelName().trim(), brand.getId())
                    .orElseThrow(AssetImportException::modelNotFound);
            return;
        }
        List<Model> matches = findModelCandidates(row);
        if (matches.isEmpty()) {
            throw AssetImportException.modelNotFound();
        }
        if (matches.size() > 1) {
            throw AssetImportException.ambiguousModel();
        }
    }

    private void validateModelType(AssetImportRowDto row) {
        Optional<Brand> brand = brandRepository.findFirstByNameIgnoreCase(row.getBrandName().trim());
        if (brand.isEmpty()) {
            return;
        }
        modelRepository.findFirstByNameIgnoreCaseAndBrandId(row.getModelName().trim(), brand.get().getId())
                .filter(existing -> !existing.getType().getName().equalsIgnoreCase(row.getTypeName().trim()))
                .ifPresent(existing -> {
                    throw AssetImportException.modelTypeMismatch();
                });
    }

    private void validateNetworkInterface(AssetImportRowDto row, Set<String> duplicateIpsInFile,
                                          Set<String> duplicateMacsInFile) {
        String ip = trimToNull(row.getIpAddress());
        String mac = trimToNull(row.getMacAddress());

        if (ip == null && mac == null) {
            return;
        }
        if (ip == null || mac == null) {
            throw AssetImportException.networkInterfaceIncomplete();
        }
        if (duplicateIpsInFile.contains(ip)) {
            throw AssetImportException.duplicateIpAddressInFile();
        }
        if (networkInterfaceRepository.existsByIpAddress(ip)) {
            throw AssetImportException.duplicateIpAddress();
        }
        if (duplicateMacsInFile.contains(mac)) {
            throw AssetImportException.duplicateMacAddressInFile();
        }
        if (networkInterfaceRepository.existsByMacAddress(mac)) {
            throw AssetImportException.duplicateMacAddress();
        }
    }

    private Location resolveLocation(AssetImportRowDto row, LocationMatchCache cache) {
        if (!isBlank(row.getCampusName())) {
            return resolveFromCampus(row, cache);
        }
        if (!isBlank(row.getBuildingName())) {
            return resolveFromBuilding(row);
        }
        if (!isBlank(row.getLocationName())) {
            return resolveFromGlobalLocation(row);
        }
        throw AssetImportException.missingLocation();
    }

    private Location resolveFromCampus(AssetImportRowDto row, LocationMatchCache cache) {
        String campusName = row.getCampusName().trim();
        Campus campus = findMatchingCampus(campusName, cache)
                .orElseGet(() -> {
                    Campus created = campusRepository.save(Campus.builder().name(campusName).build());
                    cache.registerCampus(created);
                    return created;
                });

        String buildingName = isBlank(row.getBuildingName()) ? DEFAULT_BUILDING_NAME : row.getBuildingName().trim();
        Building building = findOrCreateBuilding(buildingName, campus, cache);

        String floorName = isBlank(row.getFloorName()) ? DEFAULT_FLOOR_NAME : row.getFloorName().trim();
        Floor floor = findOrCreateFloor(floorName, building);

        String locationName = isBlank(row.getLocationName()) ? DEFAULT_LOCATION_NAME : row.getLocationName().trim();
        return findOrCreateLocation(locationName, floor);
    }

    private Location resolveFromBuilding(AssetImportRowDto row) {
        List<Building> matches = buildingRepository.findByNameIgnoreCase(row.getBuildingName().trim());
        if (matches.isEmpty()) {
            throw AssetImportException.buildingNotFound();
        }
        if (matches.size() > 1) {
            throw AssetImportException.ambiguousBuilding();
        }
        Building building = matches.get(0);

        boolean hasFloor = !isBlank(row.getFloorName());
        boolean hasLocation = !isBlank(row.getLocationName());

        if (hasFloor) {
            Floor floor = findOrCreateFloor(row.getFloorName().trim(), building);
            String locationName = hasLocation ? row.getLocationName().trim() : DEFAULT_LOCATION_NAME;
            return findOrCreateLocation(locationName, floor);
        }

        if (hasLocation) {
            String locationName = row.getLocationName().trim();
            Optional<Location> existing =
                    locationRepository.findFirstByDescriptionIgnoreCaseAndFloorBuildingId(locationName, building.getId());
            if (existing.isPresent()) {
                return existing.get();
            }
            Floor floor = findOrCreateFloor(DEFAULT_FLOOR_NAME, building);
            return findOrCreateLocation(locationName, floor);
        }

        Floor floor = findOrCreateFloor(DEFAULT_FLOOR_NAME, building);
        return findOrCreateLocation(DEFAULT_LOCATION_NAME, floor);
    }

    private Location resolveFromGlobalLocation(AssetImportRowDto row) {
        List<Location> matches = locationRepository.findByDescriptionIgnoreCase(row.getLocationName().trim());
        if (matches.isEmpty()) {
            throw AssetImportException.locationNotFound();
        }
        if (matches.size() > 1) {
            throw AssetImportException.ambiguousLocation();
        }
        return matches.get(0);
    }

    private Building findOrCreateBuilding(String name, Campus campus, LocationMatchCache cache) {
        return findMatchingBuilding(name, campus, cache)
                .orElseGet(() -> {
                    Building created = buildingRepository.save(Building.builder()
                            .name(name)
                            .campus(campus)
                            .build());
                    cache.registerBuilding(campus.getId(), created);
                    return created;
                });
    }

    private Floor findOrCreateFloor(String name, Building building) {
        return findMatchingFloor(name, building)
                .orElseGet(() -> floorRepository.save(Floor.builder()
                        .name(ImportNameNormalizer.floorCore(name))
                        .building(building)
                        .build()));
    }

    private Optional<Campus> findMatchingCampus(String rawName, LocationMatchCache cache) {
        Optional<Campus> exact = campusRepository.findFirstByNameIgnoreCase(rawName);
        if (exact.isPresent()) {
            return exact;
        }
        String core = ImportNameNormalizer.campusCore(rawName);
        if (core.isEmpty()) {
            return Optional.empty();
        }
        return cache.campuses().stream()
                .filter(campus -> core.equals(ImportNameNormalizer.campusCore(campus.getName())))
                .findFirst();
    }

    private Optional<Building> findMatchingBuilding(String rawName, Campus campus, LocationMatchCache cache) {
        Optional<Building> exact = buildingRepository.findFirstByNameIgnoreCaseAndCampusId(rawName, campus.getId());
        if (exact.isPresent()) {
            return exact;
        }
        String core = ImportNameNormalizer.buildingCore(rawName);
        if (core.isEmpty()) {
            return Optional.empty();
        }
        return cache.buildingsForCampus(campus.getId()).stream()
                .filter(building -> core.equals(ImportNameNormalizer.buildingCore(building.getName())))
                .findFirst();
    }

    private final class LocationMatchCache {
        private List<Campus> campuses;
        private final Map<UUID, List<Building>> buildingsByCampusId = new HashMap<>();

        List<Campus> campuses() {
            if (campuses == null) {
                campuses = new ArrayList<>(campusRepository.findAll());
            }
            return campuses;
        }

        void registerCampus(Campus campus) {
            if (campuses != null) {
                campuses.add(campus);
            }
        }

        List<Building> buildingsForCampus(UUID campusId) {
            return buildingsByCampusId.computeIfAbsent(campusId,
                    id -> new ArrayList<>(buildingRepository.findByCampusId(id)));
        }

        void registerBuilding(UUID campusId, Building building) {
            buildingsByCampusId.computeIfAbsent(campusId, id -> new ArrayList<>()).add(building);
        }
    }

    private Optional<Floor> findMatchingFloor(String rawName, Building building) {
        String core = ImportNameNormalizer.floorCore(rawName);
        return floorRepository.findByBuildingId(building.getId()).stream()
                .filter(floor -> core.equals(ImportNameNormalizer.floorCore(floor.getName())))
                .findFirst();
    }

    private Location findOrCreateLocation(String description, Floor floor) {
        return locationRepository.findFirstByDescriptionIgnoreCaseAndFloorId(description, floor.getId())
                .orElseGet(() -> locationRepository.save(Location.builder()
                        .description(description)
                        .floor(floor)
                        .build()));
    }

    private void validateLocation(AssetImportRowDto row) {
        if (!isBlank(row.getCampusName())) {
            return;
        }
        if (!isBlank(row.getBuildingName())) {
            List<Building> matches = buildingRepository.findByNameIgnoreCase(row.getBuildingName().trim());
            if (matches.isEmpty()) {
                throw AssetImportException.buildingNotFound();
            }
            if (matches.size() > 1) {
                throw AssetImportException.ambiguousBuilding();
            }
            return;
        }
        if (!isBlank(row.getLocationName())) {
            List<Location> matches = locationRepository.findByDescriptionIgnoreCase(row.getLocationName().trim());
            if (matches.isEmpty()) {
                throw AssetImportException.locationNotFound();
            }
            if (matches.size() > 1) {
                throw AssetImportException.ambiguousLocation();
            }
            return;
        }
        throw AssetImportException.missingLocation();
    }

    private Asset buildAsset(AssetImportRowDto row, Model model, Location location, AssetStatus status) {
        Asset asset = new Asset();
        asset.setModel(model);
        asset.setLocation(location);
        asset.setStatus(status);
        asset.setAssetNumber(row.getAssetNumber().trim());
        asset.setSerialNumber(trimToNull(row.getSerialNumber()));
        asset.setExecutingUnit(trimToNull(row.getExecutingUnit()));
        asset.setResponsibleEmployee(trimToNull(row.getResponsibleEmployee()));
        asset.setResponsibleEmployeeId(trimToNull(row.getResponsibleEmployeeId()));
        asset.setAcquisitionDate(row.getAcquisitionDate());
        asset.setWarrantyEndDate(row.getWarrantyEndDate());
        asset.setFirmwareSupportEndDate(row.getFirmwareSupportEndDate());
        asset.setLatitude(row.getLatitude());
        asset.setLongitude(row.getLongitude());
        return asset;
    }

    private void resolveNetworkInterface(AssetImportRowDto row, Asset asset) {
        String ip = trimToNull(row.getIpAddress());
        String mac = trimToNull(row.getMacAddress());

        if (ip == null && mac == null) {
            return;
        }
        networkInterfaceRepository.save(NetworkInterface.builder()
                .ipAddress(ip)
                .macAddress(mac)
                .asset(asset)
                .build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
