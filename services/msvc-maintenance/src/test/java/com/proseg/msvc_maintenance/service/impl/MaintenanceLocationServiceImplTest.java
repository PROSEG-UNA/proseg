package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceLocationServiceImplTest {

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private MaintenanceLocationServiceImpl service;

    @Captor
    private ArgumentCaptor<UUID> uuidCaptor;

    private Pageable pageable;
    private UUID campusId;
    private UUID buildingId;
    private UUID locationId;
    private UUID floorId;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 15);
        campusId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        buildingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        locationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        floorId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    }

    @Test
    @DisplayName("findCampuses delega search, page y size al cliente")
    void findCampuses_cuandoClienteResponde_mapeaPagina() {
        InventoryCampusResponseDto item = InventoryCampusResponseDto.builder().id(campusId).name("Central").build();
        when(inventoryClient.findCampuses("texto", 0, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryCampusResponseDto> page = service.findCampuses("texto", pageable);

        assertThat(page.getContent()).containsExactly(item);
        assertThat(page.getTotalElements()).isEqualTo(1L);
        verify(inventoryClient).findCampuses("texto", 0, 15);
    }

    @Test
    @DisplayName("findBuildingsByCampus delega campusId y paginación al cliente")
    void findBuildingsByCampus_cuandoClienteResponde_mapeaPagina() {
        InventoryBuildingResponseDto item = InventoryBuildingResponseDto.builder().id(buildingId).name("Torre").build();
        when(inventoryClient.findBuildingsByCampus(campusId, 0, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryBuildingResponseDto> page = service.findBuildingsByCampus(campusId, pageable);

        assertThat(page.getContent()).containsExactly(item);
        verify(inventoryClient).findBuildingsByCampus(campusId, 0, 15);
    }

    @Test
    @DisplayName("findFloorsByBuilding delega buildingId y paginación al cliente")
    void findFloorsByBuilding_cuandoClienteResponde_mapeaPagina() {
        InventoryAssetFloorResponseDto item = InventoryAssetFloorResponseDto.builder().id(floorId).name("Piso 1").build();
        when(inventoryClient.findFloorsByBuilding(buildingId, 0, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryAssetFloorResponseDto> page = service.findFloorsByBuilding(buildingId, pageable);

        assertThat(page.getContent()).containsExactly(item);
        verify(inventoryClient).findFloorsByBuilding(buildingId, 0, 15);
    }

    @Test
    @DisplayName("findLocationsByBuilding delega buildingId y paginación al cliente")
    void findLocationsByBuilding_cuandoClienteResponde_mapeaPagina() {
        InventoryAssetLocationResponseDto item =
                InventoryAssetLocationResponseDto.builder().id(locationId).description("Sala A").build();
        when(inventoryClient.findLocationsByBuilding(buildingId, 0, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryAssetLocationResponseDto> page = service.findLocationsByBuilding(buildingId, pageable);

        assertThat(page.getContent()).containsExactly(item);
        verify(inventoryClient).findLocationsByBuilding(buildingId, 0, 15);
    }

    @Test
    @DisplayName("findLocationsByCampus delega campusId y paginación al cliente")
    void findLocationsByCampus_cuandoClienteResponde_mapeaPagina() {
        InventoryAssetLocationResponseDto item =
                InventoryAssetLocationResponseDto.builder().id(locationId).description("Sala B").build();
        when(inventoryClient.findLocationsByCampus(campusId, 0, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryAssetLocationResponseDto> page = service.findLocationsByCampus(campusId, pageable);

        assertThat(page.getContent()).containsExactly(item);
        verify(inventoryClient).findLocationsByCampus(campusId, 0, 15);
    }

    @Test
    @DisplayName("findAssetsByBuilding delega buildingId, search, paginación, sort null y filtros vacíos")
    void findAssetsByBuilding_cuandoClienteResponde_mapeaPagina() {
        InventoryAssetResponseDto item = InventoryAssetResponseDto.builder().id(UUID.randomUUID()).build();
        when(inventoryClient.findAssetsByBuilding(buildingId, "activo", 0, 15, null, Map.of()))
                .thenReturn(apiPage(List.of(item), 1));

        Page<InventoryAssetResponseDto> page = service.findAssetsByBuilding(buildingId, "activo", pageable);

        assertThat(page.getContent()).containsExactly(item);
        verify(inventoryClient).findAssetsByBuilding(eq(buildingId), eq("activo"), eq(0), eq(15), isNull(), eq(Map.of()));
    }

    @Test
    @DisplayName("findAssetsByLocation delega locationId, search y paginación al cliente")
    void findAssetsByLocation_cuandoClienteResponde_mapeaPagina() {
        InventoryAssetResponseDto item = InventoryAssetResponseDto.builder().id(UUID.randomUUID()).build();
        when(inventoryClient.findAssetsByLocation(locationId, "buscar", 0, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryAssetResponseDto> page = service.findAssetsByLocation(locationId, "buscar", pageable);

        assertThat(page.getContent()).containsExactly(item);
        verify(inventoryClient).findAssetsByLocation(locationId, "buscar", 0, 15);
    }

    @Test
    @DisplayName("findCampusById delega el id al cliente y devuelve data")
    void findCampusById_cuandoClienteResponde_retornaDto() {
        InventoryCampusResponseDto dto = InventoryCampusResponseDto.builder().id(campusId).name("Central").build();
        when(inventoryClient.findCampusById(campusId)).thenReturn(new ApiResponse<>("ok", dto, 200));

        assertThat(service.findCampusById(campusId)).isSameAs(dto);
        verify(inventoryClient).findCampusById(uuidCaptor.capture());
        assertThat(uuidCaptor.getValue()).isEqualTo(campusId);
    }

    @Test
    @DisplayName("findBuildingById delega el id al cliente y devuelve data")
    void findBuildingById_cuandoClienteResponde_retornaDto() {
        InventoryBuildingResponseDto dto = InventoryBuildingResponseDto.builder().id(buildingId).name("Torre").build();
        when(inventoryClient.findBuildingById(buildingId)).thenReturn(new ApiResponse<>("ok", dto, 200));

        assertThat(service.findBuildingById(buildingId)).isSameAs(dto);
        verify(inventoryClient).findBuildingById(buildingId);
    }

    @Test
    @DisplayName("findFloorById delega el id al cliente y devuelve data")
    void findFloorById_cuandoClienteResponde_retornaDto() {
        InventoryAssetFloorResponseDto dto = InventoryAssetFloorResponseDto.builder().id(floorId).name("Piso 2").build();
        when(inventoryClient.findFloorById(floorId)).thenReturn(new ApiResponse<>("ok", dto, 200));

        assertThat(service.findFloorById(floorId)).isSameAs(dto);
        verify(inventoryClient).findFloorById(floorId);
    }

    @Test
    @DisplayName("findLocationById delega el id al cliente y devuelve data")
    void findLocationById_cuandoClienteResponde_retornaDto() {
        InventoryAssetLocationResponseDto dto =
                InventoryAssetLocationResponseDto.builder().id(locationId).description("Sala C").build();
        when(inventoryClient.findLocationById(locationId)).thenReturn(new ApiResponse<>("ok", dto, 200));

        assertThat(service.findLocationById(locationId)).isSameAs(dto);
        verify(inventoryClient).findLocationById(locationId);
    }

    @Test
    @DisplayName("findBuildingEmails delega buildingId al cliente (ignora pageable) y extrae correos")
    void findBuildingEmails_cuandoClienteResponde_retornaCorreos() {
        when(inventoryClient.findBuildingEmails(buildingId)).thenReturn(apiEmails(
                email("a@acme.com"),
                email("  "),
                email("a@acme.com"),
                email("b@acme.com")
        ));

        List<String> emails = service.findBuildingEmails(buildingId, pageable);

        assertThat(emails).containsExactly("a@acme.com", "b@acme.com");
        verify(inventoryClient).findBuildingEmails(buildingId);
    }

    @Test
    @DisplayName("findCampusEmails delega campusId al cliente (ignora pageable) y extrae correos")
    void findCampusEmails_cuandoClienteResponde_retornaCorreos() {
        when(inventoryClient.findCampusEmails(campusId)).thenReturn(apiEmails(email("campus@acme.com")));

        List<String> emails = service.findCampusEmails(campusId, pageable);

        assertThat(emails).containsExactly("campus@acme.com");
        verify(inventoryClient).findCampusEmails(campusId);
    }

    @Test
    @DisplayName("PageImpl eleva totalElements cuando el inventario reporta total menor que offset+size (página>0)")
    void findCampuses_cuandoTotalBajoEnPaginaAvanzada_PageImplAjustaTotal() {
        Pageable laterPage = PageRequest.of(2, 15);
        InventoryCampusResponseDto item = InventoryCampusResponseDto.builder().id(campusId).name("Central").build();
        when(inventoryClient.findCampuses("x", 2, 15)).thenReturn(apiPage(List.of(item), 1));

        Page<InventoryCampusResponseDto> page = service.findCampuses("x", laterPage);

        assertThat(page.getContent()).containsExactly(item);
        assertThat(page.getTotalElements()).isEqualTo(31L);
    }

    @Test
    @DisplayName("toPage con respuesta null devuelve página vacía con total 0")
    void findCampuses_cuandoRespuestaNull_devuelvePaginaVacia() {
        when(inventoryClient.findCampuses("x", 0, 15)).thenReturn(null);

        Page<InventoryCampusResponseDto> page = service.findCampuses("x", pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getPageable().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("toPage con data null devuelve página vacía con total 0")
    void findCampuses_cuandoDataNull_devuelvePaginaVacia() {
        when(inventoryClient.findCampuses("x", 0, 15)).thenReturn(new ApiResponse<>("ok", null, 200));

        Page<InventoryCampusResponseDto> page = service.findCampuses("x", pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("toPage con content null trata la lista como vacía pero conserva totalElements")
    void findCampuses_cuandoContentNull_devuelveContenidoVacio() {
        PageResponse<InventoryCampusResponseDto> pageData = PageResponse.<InventoryCampusResponseDto>builder()
                .content(null)
                .totalElements(42)
                .build();
        when(inventoryClient.findCampuses("x", 0, 15)).thenReturn(new ApiResponse<>("ok", pageData, 200));

        Page<InventoryCampusResponseDto> page = service.findCampuses("x", pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(42);
    }

    @Test
    @DisplayName("extractEmails con respuesta null devuelve lista vacía")
    void findBuildingEmails_cuandoRespuestaNull_devuelveListaVacia() {
        when(inventoryClient.findBuildingEmails(buildingId)).thenReturn(null);

        assertThat(service.findBuildingEmails(buildingId, pageable)).isEmpty();
    }

    @Test
    @DisplayName("extractEmails con data null devuelve lista vacía")
    void findCampusEmails_cuandoDataNull_devuelveListaVacia() {
        when(inventoryClient.findCampusEmails(campusId)).thenReturn(new ApiResponse<>("ok", null, 200));

        assertThat(service.findCampusEmails(campusId, pageable)).isEmpty();
    }

    @Test
    @DisplayName("findCampusById con respuesta null devuelve null (no lanza)")
    void findCampusById_cuandoRespuestaNull_retornaNull() {
        when(inventoryClient.findCampusById(campusId)).thenReturn(null);

        assertThat(service.findCampusById(campusId)).isNull();
    }

    @Test
    @DisplayName("findCampusById con data null devuelve null")
    void findCampusById_cuandoDataNull_retornaNull() {
        when(inventoryClient.findCampusById(campusId)).thenReturn(new ApiResponse<>("ok", null, 200));

        assertThat(service.findCampusById(campusId)).isNull();
    }

    @Test
    @DisplayName("fallo del cliente en método paginado propaga la excepción sin envolver")
    void findCampuses_cuandoClienteFalla_propagaExcepcion() {
        RuntimeException failure = new RuntimeException("inventory down");
        when(inventoryClient.findCampuses("x", 0, 15)).thenThrow(failure);

        assertThatThrownBy(() -> service.findCampuses("x", pageable))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("fallo Feign en findById propaga FeignException sin envolver")
    void findBuildingById_cuandoClienteFalla_propagaFeignException() {
        FeignException feignFailure = mock(FeignException.class);
        when(inventoryClient.findBuildingById(buildingId)).thenThrow(feignFailure);

        assertThatThrownBy(() -> service.findBuildingById(buildingId))
                .isSameAs(feignFailure);
    }

    @Test
    @DisplayName("fallo del cliente en correos propaga la excepción sin envolver")
    void findBuildingEmails_cuandoClienteFalla_propagaExcepcion() {
        RuntimeException failure = new RuntimeException("emails unavailable");
        when(inventoryClient.findBuildingEmails(buildingId)).thenThrow(failure);

        assertThatThrownBy(() -> service.findBuildingEmails(buildingId, pageable))
                .isSameAs(failure);
    }

    private static <T> ApiResponse<PageResponse<T>> apiPage(List<T> content, long total) {
        PageResponse<T> page = new PageResponse<>();
        page.setContent(content);
        page.setTotalElements(total);
        return new ApiResponse<>("ok", page, 200);
    }

    private static ApiResponse<List<InventoryBuildingEmailResponseDto>> apiEmails(
            InventoryBuildingEmailResponseDto... items) {
        return new ApiResponse<>("ok", List.of(items), 200);
    }

    private static InventoryBuildingEmailResponseDto email(String value) {
        return InventoryBuildingEmailResponseDto.builder().email(value).build();
    }
}
