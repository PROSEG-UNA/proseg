package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryCampusResponseDto;
import com.proseg.msvc_maintenance.service.MaintenanceLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaintenanceLocationControllerTest {

    @Mock
    private MaintenanceLocationService maintenanceLocationService;

    private MockMvc mockMvc;
    private UUID campusId;
    private UUID buildingId;
    private UUID floorId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new MaintenanceLocationController(maintenanceLocationService),
                "routes.maintenance-locations",
                ControllerMockMvcSupport.LOCATIONS);
        campusId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        locationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET campuses devuelve página")
    void findCampuses_cuandoHayDatos_retornaOk() throws Exception {
        InventoryCampusResponseDto dto = InventoryCampusResponseDto.builder().id(campusId).name("Campus").build();
        when(maintenanceLocationService.findCampuses(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Campus"));
    }

    @Test
    @DisplayName("GET campuses propaga IllegalArgumentException como 400")
    void findCampuses_cuandoServicioFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findCampuses(any(), any()))
                .thenThrow(new IllegalArgumentException("filtro inválido"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET buildings by campus devuelve página")
    void findBuildingsByCampus_cuandoHayDatos_retornaOk() throws Exception {
        InventoryBuildingResponseDto dto = InventoryBuildingResponseDto.builder().id(buildingId).name("Edificio").build();
        when(maintenanceLocationService.findBuildingsByCampus(eq(campusId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId + "/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(buildingId.toString()));
    }

    @Test
    @DisplayName("GET buildings by campus propaga error")
    void findBuildingsByCampus_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findBuildingsByCampus(eq(campusId), any()))
                .thenThrow(new IllegalArgumentException("campus inválido"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId + "/buildings")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET floors by building devuelve página")
    void findFloorsByBuilding_cuandoHayDatos_retornaOk() throws Exception {
        InventoryAssetFloorResponseDto dto = InventoryAssetFloorResponseDto.builder().id(floorId).build();
        when(maintenanceLocationService.findFloorsByBuilding(eq(buildingId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/floors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(floorId.toString()));
    }

    @Test
    @DisplayName("GET floors propaga error")
    void findFloorsByBuilding_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findFloorsByBuilding(eq(buildingId), any()))
                .thenThrow(new IllegalArgumentException("edificio inválido"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/floors")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET locations by building devuelve página")
    void findLocationsByBuilding_cuandoHayDatos_retornaOk() throws Exception {
        InventoryAssetLocationResponseDto dto = InventoryAssetLocationResponseDto.builder().id(locationId).build();
        when(maintenanceLocationService.findLocationsByBuilding(eq(buildingId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(locationId.toString()));
    }

    @Test
    @DisplayName("GET locations by building propaga error")
    void findLocationsByBuilding_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findLocationsByBuilding(eq(buildingId), any()))
                .thenThrow(new IllegalArgumentException("error"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/locations")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET locations by campus devuelve página")
    void findLocationsByCampus_cuandoHayDatos_retornaOk() throws Exception {
        InventoryAssetLocationResponseDto dto = InventoryAssetLocationResponseDto.builder().id(locationId).build();
        when(maintenanceLocationService.findLocationsByCampus(eq(campusId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId + "/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(locationId.toString()));
    }

    @Test
    @DisplayName("GET locations by campus propaga error")
    void findLocationsByCampus_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findLocationsByCampus(eq(campusId), any()))
                .thenThrow(new IllegalArgumentException("error"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId + "/locations")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET campus by id devuelve DTO")
    void findCampusById_cuandoExiste_retornaOk() throws Exception {
        InventoryCampusResponseDto dto = InventoryCampusResponseDto.builder().id(campusId).name("C1").build();
        when(maintenanceLocationService.findCampusById(campusId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("C1"));
    }

    @Test
    @DisplayName("GET campus by id propaga error")
    void findCampusById_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findCampusById(campusId)).thenThrow(new IllegalArgumentException("no existe"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId)),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET building by id devuelve DTO")
    void findBuildingById_cuandoExiste_retornaOk() throws Exception {
        InventoryBuildingResponseDto dto = InventoryBuildingResponseDto.builder().id(buildingId).build();
        when(maintenanceLocationService.findBuildingById(buildingId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(buildingId.toString()));
    }

    @Test
    @DisplayName("GET building by id propaga error")
    void findBuildingById_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findBuildingById(buildingId)).thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId)),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET floor by id devuelve DTO")
    void findFloorById_cuandoExiste_retornaOk() throws Exception {
        InventoryAssetFloorResponseDto dto = InventoryAssetFloorResponseDto.builder().id(floorId).build();
        when(maintenanceLocationService.findFloorById(floorId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/floors/" + floorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(floorId.toString()));
    }

    @Test
    @DisplayName("GET floor by id propaga error")
    void findFloorById_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findFloorById(floorId)).thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/floors/" + floorId)),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET location by id devuelve DTO")
    void findLocationById_cuandoExiste_retornaOk() throws Exception {
        InventoryAssetLocationResponseDto dto = InventoryAssetLocationResponseDto.builder().id(locationId).build();
        when(maintenanceLocationService.findLocationById(locationId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/locations/" + locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(locationId.toString()));
    }

    @Test
    @DisplayName("GET location by id propaga error")
    void findLocationById_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findLocationById(locationId)).thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/locations/" + locationId)),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET assets by location devuelve página")
    void findAssetsByLocation_cuandoHayDatos_retornaOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        InventoryAssetResponseDto dto = InventoryAssetResponseDto.builder().id(assetId).build();
        when(maintenanceLocationService.findAssetsByLocation(eq(locationId), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/locations/" + locationId + "/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(assetId.toString()));
    }

    @Test
    @DisplayName("GET assets by location propaga error")
    void findAssetsByLocation_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findAssetsByLocation(eq(locationId), any(), any()))
                .thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/locations/" + locationId + "/assets")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET assets by building devuelve página")
    void findAssetsByBuilding_cuandoHayDatos_retornaOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        InventoryAssetResponseDto dto = InventoryAssetResponseDto.builder().id(assetId).build();
        when(maintenanceLocationService.findAssetsByBuilding(eq(buildingId), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 200), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(assetId.toString()));
    }

    @Test
    @DisplayName("GET assets by building propaga error")
    void findAssetsByBuilding_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findAssetsByBuilding(eq(buildingId), any(), any()))
                .thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/assets")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET building emails devuelve lista")
    void findBuildingEmails_cuandoHayDatos_retornaOk() throws Exception {
        when(maintenanceLocationService.findBuildingEmails(eq(buildingId), any()))
                .thenReturn(List.of("a@example.com"));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("a@example.com"));
    }

    @Test
    @DisplayName("GET building emails propaga error")
    void findBuildingEmails_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findBuildingEmails(eq(buildingId), any()))
                .thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/buildings/" + buildingId + "/emails")),
                400,
                "INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("GET campus emails devuelve lista")
    void findCampusEmails_cuandoHayDatos_retornaOk() throws Exception {
        when(maintenanceLocationService.findCampusEmails(eq(campusId), any()))
                .thenReturn(List.of("campus@example.com"));

        mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId + "/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("campus@example.com"));
    }

    @Test
    @DisplayName("GET campus emails propaga error")
    void findCampusEmails_cuandoFalla_retornaBadRequest() throws Exception {
        when(maintenanceLocationService.findCampusEmails(eq(campusId), any()))
                .thenThrow(new IllegalArgumentException("no"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.LOCATIONS + "/campuses/" + campusId + "/emails")),
                400,
                "INVALID_ARGUMENT");
    }
}
