package com.proseg.msvcinventory;

import com.proseg.msvcinventory.dto.request.AssetImportRowDto;
import com.proseg.msvcinventory.entity.*;
import com.proseg.msvcinventory.importer.ImportPeopleContext;
import com.proseg.msvcinventory.importer.ResponsibleResolver;
import com.proseg.msvcinventory.repository.*;
import com.proseg.msvcinventory.service.impl.AssetImportRowProcessor;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetImportRowProcessorTest {

    @Mock private AssetRepository assetRepository;
    @Mock private TypeRepository typeRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private ModelRepository modelRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private NetworkInterfaceRepository networkInterfaceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ExecutingUnitRepository executingUnitRepository;
    @Mock private Validator validator;

    private AssetImportRowProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AssetImportRowProcessor(
                assetRepository,
                typeRepository,
                brandRepository,
                modelRepository,
                campusRepository,
                buildingRepository,
                floorRepository,
                locationRepository,
                networkInterfaceRepository,
                employeeRepository,
                executingUnitRepository,
                new ResponsibleResolver(),
                validator);
    }

    @Test
    void validateRow_allowsValidAssetImport() {
        AssetImportRowDto row = validRow();
        Type type = Type.builder().id(UUID.randomUUID()).name("Laptop").requiresNetworkInterface(true).build();
        Brand brand = Brand.builder().id(UUID.randomUUID()).name("Dell").build();
        Model model = Model.builder().id(UUID.randomUUID()).name("Latitude 5420").brand(brand).type(type).build();

        when(validator.validate(any())).thenReturn(Set.of());
        when(brandRepository.findFirstByNameIgnoreCase("Dell")).thenReturn(Optional.of(brand));
        when(modelRepository.findFirstByNameIgnoreCaseAndBrandId("Latitude 5420", brand.getId())).thenReturn(Optional.of(model));
        when(assetRepository.existsByAssetNumber("A-500")).thenReturn(false);
        when(assetRepository.existsBySerialNumber("SER-500")).thenReturn(false);
        when(networkInterfaceRepository.existsByIpAddress("10.0.0.50")).thenReturn(false);
        when(networkInterfaceRepository.existsByMacAddress("AA:BB:CC:DD:EE:FF")).thenReturn(false);

        processor.validateRow(row, Set.of(), Set.of(), Set.of(), Set.of(), new ImportPeopleContext(employeeRepository, executingUnitRepository, false));

        verify(validator).validate(any());
    }

    @Test
    void validateRow_rejectsDuplicateAssetNumberInFile() {
        AssetImportRowDto row = validRow();
        when(validator.validate(any())).thenReturn(Set.of());
        when(brandRepository.findFirstByNameIgnoreCase("Dell")).thenReturn(Optional.of(Brand.builder().id(UUID.randomUUID()).name("Dell").build()));
        when(modelRepository.findFirstByNameIgnoreCaseAndBrandId(any(), any())).thenReturn(Optional.of(Model.builder().id(UUID.randomUUID()).name("Latitude 5420").brand(Brand.builder().id(UUID.randomUUID()).name("Dell").build()).type(Type.builder().id(UUID.randomUUID()).name("Laptop").requiresNetworkInterface(true).build()).build()));

        assertThatThrownBy(() -> processor.validateRow(row, Set.of("A-500"), Set.of(), Set.of(), Set.of(),
                new ImportPeopleContext(employeeRepository, executingUnitRepository, false)))
                .isNotNull();
    }

    @Test
    void planCreations_collectsRequiredPendingRecords() {
        AssetImportRowDto row = validRow();
        when(typeRepository.findFirstByNameIgnoreCase("Laptop")).thenReturn(Optional.empty());
        when(brandRepository.findFirstByNameIgnoreCase("Dell")).thenReturn(Optional.empty());

        List<?> pending = processor.planCreations(List.of(row));

        assertThat(pending).isNotEmpty();
    }

    @Test
    void persistRows_createsAssetAndNetworkInterface() {
        AssetImportRowDto row = validRow();
        Type type = Type.builder().id(UUID.randomUUID()).name("Laptop").requiresNetworkInterface(true).build();
        Brand brand = Brand.builder().id(UUID.randomUUID()).name("Dell").build();
        Model model = Model.builder().id(UUID.randomUUID()).name("Latitude 5420").brand(brand).type(type).build();
        Campus campus = Campus.builder().id(UUID.randomUUID()).name("Campus A").build();
        Building building = Building.builder().id(UUID.randomUUID()).name("B1").campus(campus).build();
        Floor floor = Floor.builder().id(UUID.randomUUID()).name("1").building(building).build();
        Location location = Location.builder().id(UUID.randomUUID()).description("-" ).floor(floor).build();
        Asset savedAsset = new Asset();
        savedAsset.setId(UUID.randomUUID());
        savedAsset.setAssetNumber("A-500");
        savedAsset.setStatus(com.proseg.msvcinventory.entity.enums.AssetStatus.APROBADO);
        savedAsset.setModel(model);
        savedAsset.setLocation(location);

        when(typeRepository.findFirstByNameIgnoreCase("Laptop")).thenReturn(Optional.of(type));
        when(brandRepository.findFirstByNameIgnoreCase("Dell")).thenReturn(Optional.of(brand));
        when(modelRepository.findFirstByNameIgnoreCaseAndBrandId("Latitude 5420", brand.getId())).thenReturn(Optional.of(model));
        when(campusRepository.findFirstByNameIgnoreCase("Campus A")).thenReturn(Optional.of(campus));
        when(buildingRepository.findFirstByNameIgnoreCaseAndCampusId("B1", campus.getId())).thenReturn(Optional.of(building));
        when(floorRepository.findByBuildingId(building.getId())).thenReturn(List.of(floor));
        when(locationRepository.findFirstByDescriptionIgnoreCaseAndFloorId("-", floor.getId())).thenReturn(Optional.of(location));
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);
        when(networkInterfaceRepository.save(any(NetworkInterface.class))).thenAnswer(invocation -> invocation.getArgument(0));

        processor.persistRows(List.of(row));

        verify(assetRepository).save(any(Asset.class));
        verify(networkInterfaceRepository).save(any(NetworkInterface.class));
    }

    private AssetImportRowDto validRow() {
        return AssetImportRowDto.builder()
                .rowNumber(1)
                .assetNumber("A-500")
                .status("APROBADO")
                .typeName("Laptop")
                .brandName("Dell")
                .modelName("Latitude 5420")
                .serialNumber("SER-500")
                .campusName("Campus A")
                .buildingName("B1")
                .floorName("1")
                .locationName("-")
                .acquisitionDate(LocalDate.now().minusDays(10))
                .warrantyEndDate(LocalDate.now().plusDays(365))
                .firmwareSupportEndDate(LocalDate.now().plusDays(180))
                .ipAddress("10.0.0.50")
                .macAddress("AA:BB:CC:DD:EE:FF")
                .latitude(BigDecimal.valueOf(9.99))
                .longitude(BigDecimal.valueOf(-84.00))
                .build();
    }
}
