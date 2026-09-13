package com.proseg.msvcinventory;

import com.proseg.msvcinventory.dto.request.*;
import com.proseg.msvcinventory.dto.response.*;
import com.proseg.msvcinventory.entity.*;
import com.proseg.msvcinventory.entity.enums.AssetStatus;
import com.proseg.msvcinventory.exception.*;
import com.proseg.msvcinventory.mapper.*;
import com.proseg.msvcinventory.repository.*;
import com.proseg.msvcinventory.service.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private BrandRepository brandRepository;
    @Mock private ModelRepository modelRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private TypeRepository typeRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private NetworkInterfaceRepository networkInterfaceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ExecutingUnitRepository executingUnitRepository;
    @Mock private AssetArchiveRepository assetArchiveRepository;
    @Mock private AssetComponentRepository assetComponentRepository;
    @Mock private BrandMapper brandMapper;
    @Mock private CampusMapper campusMapper;
    @Mock private TypeMapper typeMapper;
    @Mock private BuildingMapper buildingMapper;
    @Mock private ModelMapper modelMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private AlarmSensorMapper alarmSensorMapper;
    @Mock private LocationMapper locationMapper;
    @Mock private NetworkInterfaceMapper networkInterfaceMapper;
    @Mock private ExecutingUnitMapper executingUnitMapper;

    @Test
    void brandService_create_success() {
        BrandServiceImpl service = new BrandServiceImpl(brandRepository, modelRepository, brandMapper);
        BrandRequestDto request = new BrandRequestDto();
        request.setName("Dell");
        Brand brand = Brand.builder().name("Dell").build();
        BrandResponseDto response = new BrandResponseDto();

        when(brandRepository.existsByNameIgnoreCase("Dell")).thenReturn(false);
        when(brandMapper.toEntity(request)).thenReturn(brand);
        when(brandRepository.save(brand)).thenReturn(brand);
        when(brandMapper.toResponse(brand)).thenReturn(response);

        BrandResponseDto result = service.create(request);

        assertThat(result).isSameAs(response);
        verify(brandRepository).save(brand);
    }

    @Test
    void brandService_delete_whenModelInUse_throws() {
        BrandServiceImpl service = new BrandServiceImpl(brandRepository, modelRepository, brandMapper);
        UUID id = UUID.randomUUID();
        Brand brand = Brand.builder().id(id).name("Dell").build();

        when(brandRepository.findById(id)).thenReturn(Optional.of(brand));
        when(modelRepository.existsByBrandId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BrandException.class)
                .hasMessageContaining("tiene modelos de activo asociados");
    }

    @Test
    void campusService_create_success() {
        CampusServiceImpl service = new CampusServiceImpl(campusRepository, buildingRepository, campusMapper);
        CampusRequestDto request = new CampusRequestDto();
        request.setName("Campus Central");
        Campus campus = Campus.builder().name("Campus Central").build();
        CampusResponseDto response = new CampusResponseDto();

        when(campusRepository.existsByNameIgnoreCase("Campus Central")).thenReturn(false);
        when(campusMapper.toEntity(request)).thenReturn(campus);
        when(campusRepository.save(campus)).thenReturn(campus);
        when(campusMapper.toResponse(campus)).thenReturn(response);

        CampusResponseDto result = service.create(request);

        assertThat(result).isSameAs(response);
        verify(campusRepository).save(campus);
    }

    @Test
    void typeService_update_whenDisablingNetworkInterface_softDeletesNi() {
        TypeServiceImpl service = new TypeServiceImpl(typeRepository, modelRepository, assetRepository,
                networkInterfaceRepository, typeMapper);
        UUID id = UUID.randomUUID();
        Type type = Type.builder().id(id).name("Switch").requiresNetworkInterface(true).build();
        TypeRequestDto request = new TypeRequestDto();
        request.setName("Switch");
        request.setRequiresNetworkInterface(false);

        when(typeRepository.findById(id)).thenReturn(Optional.of(type));
        when(typeRepository.existsByNameIgnoreCaseAndIdNot("Switch", id)).thenReturn(false);
        doAnswer(invocation -> {
            type.setRequiresNetworkInterface(false);
            return null;
        }).when(typeMapper).updateEntityFromRequest(request, type);
        when(typeRepository.save(type)).thenReturn(type);

        TypeResponseDto response = new TypeResponseDto();
        when(typeMapper.toResponse(type)).thenReturn(response);

        TypeResponseDto result = service.update(id, request);

        assertThat(result).isSameAs(response);
        verify(networkInterfaceRepository).softDeleteAllByAssetTypeId(id);
    }

    @Test
    void buildingService_create_success() {
        BuildingServiceImpl service = new BuildingServiceImpl(buildingRepository, campusRepository, floorRepository, buildingMapper);
        UUID campusId = UUID.randomUUID();
        BuildingRequestDto request = new BuildingRequestDto();
        request.setCampusId(campusId);
        request.setName("Edificio A");
        Campus campus = Campus.builder().id(campusId).name("Campus").build();
        Building building = Building.builder().name("Edificio A").campus(campus).build();
        BuildingResponseDto response = new BuildingResponseDto();

        when(campusRepository.findById(campusId)).thenReturn(Optional.of(campus));
        when(buildingRepository.existsByNameIgnoreCaseAndCampusId("Edificio A", campusId)).thenReturn(false);
        when(buildingMapper.toEntity(request)).thenReturn(building);
        when(buildingRepository.save(building)).thenReturn(building);
        when(buildingMapper.toResponse(building)).thenReturn(response);

        BuildingResponseDto result = service.create(request);

        assertThat(result).isSameAs(response);
        verify(buildingRepository).save(building);
    }

    @Test
    void locationService_create_createsMissingFloor() {
        LocationServiceImpl service = new LocationServiceImpl(
                locationRepository, campusRepository, buildingRepository, floorRepository, assetRepository, locationMapper);
        UUID campusId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        LocationRequestDto request = new LocationRequestDto();
        request.setCampusId(campusId);
        request.setBuildingId(buildingId);
        request.setFloorNumber(3);
        request.setDescription("Sala 301");

        Campus campus = Campus.builder().id(campusId).name("Campus").build();
        Building building = Building.builder().id(buildingId).name("Edificio A").campus(campus).build();
        Floor newFloor = Floor.builder().id(UUID.randomUUID()).name("3").building(building).build();
        Location location = Location.builder().id(UUID.randomUUID()).description("Sala 301").floor(newFloor).build();

        when(campusRepository.existsById(campusId)).thenReturn(true);
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(floorRepository.findByNameAndBuildingId("3", buildingId)).thenReturn(Optional.empty());
        when(floorRepository.save(any(Floor.class))).thenReturn(newFloor);
        when(locationRepository.existsByDescriptionIgnoreCaseAndFloorId("Sala 301", newFloor.getId())).thenReturn(false);
        when(locationMapper.toEntity(request)).thenReturn(new Location());
        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(locationMapper.toResponse(location)).thenReturn(new LocationResponseDto());

        LocationResponseDto result = service.create(request);

        assertThat(result).isNotNull();
        verify(floorRepository).save(any(Floor.class));
    }

    @Test
    void modelService_create_success() {
        ModelServiceImpl service = new ModelServiceImpl(modelRepository, brandRepository, typeRepository, assetRepository, modelMapper);
        UUID brandId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        ModelRequestDto request = new ModelRequestDto();
        request.setBrandId(brandId);
        request.setTypeId(typeId);
        request.setName("Latitude 5420");

        Brand brand = Brand.builder().id(brandId).name("Dell").build();
        Type type = Type.builder().id(typeId).name("Laptop").requiresNetworkInterface(true).build();
        Model model = Model.builder().id(UUID.randomUUID()).name("Latitude 5420").brand(brand).type(type).build();
        ModelResponseDto response = new ModelResponseDto();

        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(typeRepository.findById(typeId)).thenReturn(Optional.of(type));
        when(modelRepository.existsByNameIgnoreCaseAndBrandId("Latitude 5420", brandId)).thenReturn(false);
        when(modelMapper.toEntity(request)).thenReturn(model);
        when(modelRepository.save(model)).thenReturn(model);
        when(modelMapper.toResponse(model)).thenReturn(response);

        ModelResponseDto result = service.create(request);

        assertThat(result).isSameAs(response);
        verify(modelRepository).save(model);
    }

    @Test
    void employeeService_create_trimsIdentificationAndRejectsDuplicates() {
        EmployeeServiceImpl service = new EmployeeServiceImpl(employeeRepository, assetRepository, employeeMapper);
        EmployeeRequestDto request = new EmployeeRequestDto();
        request.setName("Ana Perez");
        request.setIdentification(" 12345 ");

        Employee employee = Employee.builder().name("Ana Perez").identification("12345").build();
        EmployeeResponseDto response = new EmployeeResponseDto();

        when(employeeRepository.existsByNameIgnoreCase("Ana Perez")).thenReturn(false);
        when(employeeRepository.existsByIdentificationIgnoreCase("12345")).thenReturn(false);
        when(employeeMapper.toEntity(any(EmployeeRequestDto.class))).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponseDto result = service.create(request);

        assertThat(result).isSameAs(response);
        assertThat(request.getIdentification()).isEqualTo("12345");
    }

    @Test
    void executingUnitService_delete_inUse_throws() {
        ExecutingUnitServiceImpl service = new ExecutingUnitServiceImpl(executingUnitRepository, assetRepository, executingUnitMapper);
        UUID id = UUID.randomUUID();
        ExecutingUnit unit = ExecutingUnit.builder().id(id).name("Inventario").build();

        when(executingUnitRepository.findById(id)).thenReturn(Optional.of(unit));
        when(assetRepository.existsByExecutingUnitId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ExecutingUnitException.class);
    }

    @Test
    void networkInterfaceService_create_success() {
        NetworkInterfaceServiceImpl service = new NetworkInterfaceServiceImpl(networkInterfaceRepository, assetRepository, networkInterfaceMapper);
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setAssetNumber("A-100");
        NetworkInterfaceRequestDto request = new NetworkInterfaceRequestDto();
        request.setAssetId(assetId);
        request.setIpAddress("10.0.0.11");
        request.setMacAddress("AA:BB:CC:DD:EE:FF");

        NetworkInterface networkInterface = NetworkInterface.builder().id(UUID.randomUUID()).asset(asset).ipAddress("10.0.0.11").macAddress("AA:BB:CC:DD:EE:FF").build();
        NetworkInterfaceResponseDto response = new NetworkInterfaceResponseDto();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(networkInterfaceRepository.existsByAssetId(assetId)).thenReturn(false);
        when(networkInterfaceRepository.existsByIpAddress("10.0.0.11")).thenReturn(false);
        when(networkInterfaceRepository.existsByMacAddress("AA:BB:CC:DD:EE:FF")).thenReturn(false);
        when(networkInterfaceMapper.toEntity(request)).thenReturn(networkInterface);
        when(networkInterfaceRepository.save(networkInterface)).thenReturn(networkInterface);
        when(networkInterfaceMapper.toResponse(networkInterface)).thenReturn(response);

        NetworkInterfaceResponseDto result = service.create(request);

        assertThat(result).isSameAs(response);
        verify(networkInterfaceRepository).save(networkInterface);
    }

    @Test
    void alarmSensorService_create_rejectsDecommissionDateWhenApproved() {
        AlarmSensorServiceImpl service = new AlarmSensorServiceImpl(
                mock(AlarmSensorRepository.class), modelRepository, locationRepository,
                executingUnitRepository, employeeRepository, alarmSensorMapper);
        UUID modelId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        AlarmSensorRequestDto request = new AlarmSensorRequestDto();
        request.setModelId(modelId);
        request.setLocationId(locationId);
        request.setStatus(AssetStatus.APROBADO);
        request.setDecommissionDate(java.time.LocalDate.now());

        Model model = Model.builder().id(modelId).name("Sensor").build();
        Location location = Location.builder().id(locationId).description("Sala 1").build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AssetException.class);
    }
}
