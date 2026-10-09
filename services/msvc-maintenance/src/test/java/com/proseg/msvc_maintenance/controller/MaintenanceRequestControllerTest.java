package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.dto.request.MaintenanceRequestCancelRequestDto;
import com.proseg.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.proseg.msvc_maintenance.exception.MaintenanceRequestException;
import com.proseg.msvc_maintenance.service.MaintenanceRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestControllerTest {

    @Mock
    private MaintenanceRequestService maintenanceRequestService;

    private MockMvc mockMvc;
    private UUID requestId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new MaintenanceRequestController(maintenanceRequestService),
                "routes.requests",
                ControllerMockMvcSupport.REQUESTS);
        requestId = UUID.randomUUID();
        companyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST create devuelve 201 y envía DTO al servicio")
    void create_cuandoPayloadValido_retornaCreated() throws Exception {
        MaintenanceRequestRequestDto body = ControllerMockMvcSupport.validMaintenanceRequest();
        MaintenanceRequestResponseDto response = MaintenanceRequestResponseDto.builder()
                .id(UUID.randomUUID())
                .build();
        ArgumentCaptor<MaintenanceRequestRequestDto> captor = ArgumentCaptor.forClass(MaintenanceRequestRequestDto.class);
        when(maintenanceRequestService.create(any())).thenReturn(response);

        mockMvc.perform(post(ControllerMockMvcSupport.REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()));

        verify(maintenanceRequestService).create(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo(body.getCompanyId());
    }

    @Test
    @DisplayName("POST create propaga error del servicio")
    void create_cuandoServicioFalla_retornaConflict() throws Exception {
        MaintenanceRequestRequestDto body = ControllerMockMvcSupport.validMaintenanceRequest();
        when(maintenanceRequestService.create(any())).thenThrow(MaintenanceRequestException.companyNotAllowed());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                403,
                "MAINTENANCE_REQUEST_COMPANY_NOT_ALLOWED");
    }

    @Test
    @DisplayName("GET by id devuelve solicitud")
    void findById_cuandoExiste_retornaOk() throws Exception {
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        when(maintenanceRequestService.findById(requestId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS + "/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId.toString()));
    }

    @Test
    @DisplayName("GET by id propaga not found")
    void findById_cuandoNoExiste_retornaNotFound() throws Exception {
        when(maintenanceRequestService.findById(requestId)).thenThrow(MaintenanceRequestException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS + "/" + requestId)),
                404,
                "MAINTENANCE_REQUEST_NOT_FOUND");
    }

    @Test
    @DisplayName("GET listado paginado devuelve content")
    void findAll_cuandoHayDatos_retornaOk() throws Exception {
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        when(maintenanceRequestService.findAll(eq(null), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(requestId.toString()));
    }

    @Test
    @DisplayName("GET listado propaga error del servicio")
    void findAll_cuandoServicioFalla_retornaBadRequest() throws Exception {
        when(maintenanceRequestService.findAll(any(), any(), any()))
                .thenThrow(MaintenanceRequestException.registeredEmailsUnavailable());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS)),
                503,
                "MAINTENANCE_REQUEST_EMAILS_UNAVAILABLE");
    }

    @Test
    @DisplayName("GET assets disponibles devuelve página")
    void findAvailableAssets_cuandoHayDatos_retornaOk() throws Exception {
        UUID assetUuid = UUID.randomUUID();
        MaintenanceAssetOptionDto asset = MaintenanceAssetOptionDto.builder()
                .id(assetUuid)
                .build();
        when(maintenanceRequestService.findAvailableAssets(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(asset), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS + "/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(assetUuid.toString()));
    }

    @Test
    @DisplayName("GET assets propaga error del servicio")
    void findAvailableAssets_cuandoServicioFalla_retornaBadGateway() throws Exception {
        when(maintenanceRequestService.findAvailableAssets(any(), any()))
                .thenThrow(MaintenanceRequestException.emailNotRegistered("bad@example.com"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS + "/assets")),
                400,
                "MAINTENANCE_REQUEST_EMAIL_NOT_REGISTERED");
    }

    @Test
    @DisplayName("GET by company devuelve solicitudes")
    void findByCompanyId_cuandoHayDatos_retornaOk() throws Exception {
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        when(maintenanceRequestService.findByCompanyId(eq(companyId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS + "/company/" + companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(requestId.toString()));
    }

    @Test
    @DisplayName("GET by company propaga not found de empresa")
    void findByCompanyId_cuandoEmpresaInvalida_retornaNotFound() throws Exception {
        when(maintenanceRequestService.findByCompanyId(eq(companyId), any()))
                .thenThrow(MaintenanceRequestException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REQUESTS + "/company/" + companyId)),
                404,
                "MAINTENANCE_REQUEST_NOT_FOUND");
    }

    @Test
    @DisplayName("PUT update devuelve solicitud actualizada")
    void update_cuandoPayloadValido_retornaOk() throws Exception {
        MaintenanceRequestRequestDto body = ControllerMockMvcSupport.validMaintenanceRequest();
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        when(maintenanceRequestService.update(eq(requestId), any())).thenReturn(dto);

        mockMvc.perform(put(ControllerMockMvcSupport.REQUESTS + "/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId.toString()));
    }

    @Test
    @DisplayName("PUT update propaga not found")
    void update_cuandoNoExiste_retornaNotFound() throws Exception {
        MaintenanceRequestRequestDto body = ControllerMockMvcSupport.validMaintenanceRequest();
        when(maintenanceRequestService.update(eq(requestId), any())).thenThrow(MaintenanceRequestException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(put(ControllerMockMvcSupport.REQUESTS + "/" + requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                404,
                "MAINTENANCE_REQUEST_NOT_FOUND");
    }

    @Test
    @DisplayName("PATCH cancel envía motivo al servicio")
    void cancel_cuandoHayMotivo_retornaOk() throws Exception {
        MaintenanceRequestCancelRequestDto cancel = MaintenanceRequestCancelRequestDto.builder()
                .reason("Cliente canceló")
                .build();
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        when(maintenanceRequestService.cancel(eq(requestId), any())).thenReturn(dto);

        mockMvc.perform(patch(ControllerMockMvcSupport.REQUESTS + "/" + requestId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(cancel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId.toString()));

        verify(maintenanceRequestService).cancel(eq(requestId), reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).isEqualTo("Cliente canceló");
    }

    @Test
    @DisplayName("PATCH cancel propaga error del servicio")
    void cancel_cuandoNoPermitido_retornaBadRequest() throws Exception {
        when(maintenanceRequestService.cancel(eq(requestId), any()))
                .thenThrow(MaintenanceRequestException.invalidStatusTransition(
                        com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus.COMPLETED,
                        com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus.CANCELLED));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(patch(ControllerMockMvcSupport.REQUESTS + "/" + requestId + "/cancel")),
                409,
                "MAINTENANCE_REQUEST_INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("DELETE elimina solicitud")
    void delete_cuandoExiste_retornaOk() throws Exception {
        mockMvc.perform(delete(ControllerMockMvcSupport.REQUESTS + "/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(maintenanceRequestService).delete(requestId);
    }

    @Test
    @DisplayName("DELETE propaga not found")
    void delete_cuandoNoExiste_retornaNotFound() throws Exception {
        org.mockito.Mockito.doThrow(MaintenanceRequestException.notFound())
                .when(maintenanceRequestService).delete(requestId);

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(delete(ControllerMockMvcSupport.REQUESTS + "/" + requestId)),
                404,
                "MAINTENANCE_REQUEST_NOT_FOUND");
    }
}
