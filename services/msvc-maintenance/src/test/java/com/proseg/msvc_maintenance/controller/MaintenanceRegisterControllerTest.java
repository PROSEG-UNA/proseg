package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.dto.request.MaintenanceRecordRequestDto;
import com.proseg.msvc_maintenance.dto.request.MaintenanceRegisterUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRegisterResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.proseg.msvc_maintenance.exception.MaintenanceRegisterException;
import com.proseg.msvc_maintenance.service.MaintenanceRecordService;
import com.proseg.msvc_maintenance.service.MaintenanceRegisterService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaintenanceRegisterControllerTest {

    @Mock
    private MaintenanceRegisterService maintenanceRegisterService;
    @Mock
    private MaintenanceRecordService maintenanceRecordService;

    private MockMvc mockMvc;
    private UUID registerId;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new MaintenanceRegisterController(maintenanceRegisterService, maintenanceRecordService),
                "routes.registers",
                ControllerMockMvcSupport.REGISTERS);
        registerId = UUID.randomUUID();
        requestId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET assigned devuelve solicitudes paginadas")
    void findAssigned_cuandoHayDatos_retornaOk() throws Exception {
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        when(maintenanceRegisterService.findAssigned(eq("kc-user-subject"), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(requestId.toString()));
    }

    @Test
    @DisplayName("GET assigned propaga error del servicio")
    void findAssigned_cuandoServicioFalla_retornaForbidden() throws Exception {
        when(maintenanceRegisterService.findAssigned(any(), any(), any()))
                .thenThrow(MaintenanceRegisterException.userWithoutCompany());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/assigned")),
                403,
                "MAINTENANCE_USER_WITHOUT_COMPANY");
    }

    @Test
    @DisplayName("GET history devuelve historial paginado")
    void findHistory_cuandoHayDatos_retornaOk() throws Exception {
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().id(requestId).build();
        when(maintenanceRegisterService.findHistory(eq("kc-user-subject"), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(requestId.toString()));
    }

    @Test
    @DisplayName("GET history propaga not found")
    void findHistory_cuandoServicioFalla_retornaNotFound() throws Exception {
        when(maintenanceRegisterService.findHistory(any(), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/history")),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }

    @Test
    @DisplayName("GET by-request devuelve registro")
    void getOrCreateByRequest_cuandoExiste_retornaOk() throws Exception {
        MaintenanceRegisterResponseDto dto = MaintenanceRegisterResponseDto.builder().id(registerId).build();
        when(maintenanceRegisterService.getOrCreateByRequest(requestId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/by-request/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(registerId.toString()));
    }

    @Test
    @DisplayName("GET by-request propaga error")
    void getOrCreateByRequest_cuandoFalla_retornaConflict() throws Exception {
        when(maintenanceRegisterService.getOrCreateByRequest(requestId))
                .thenThrow(MaintenanceRegisterException.notPending());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/by-request/" + requestId)),
                409,
                "MAINTENANCE_REGISTER_NOT_PENDING");
    }

    @Test
    @DisplayName("PUT update envía DTO al servicio")
    void update_cuandoPayloadValido_retornaOk() throws Exception {
        MaintenanceRegisterUpdateRequestDto body = MaintenanceRegisterUpdateRequestDto.builder()
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build();
        MaintenanceRegisterResponseDto dto = MaintenanceRegisterResponseDto.builder().id(registerId).build();
        ArgumentCaptor<MaintenanceRegisterUpdateRequestDto> captor =
                ArgumentCaptor.forClass(MaintenanceRegisterUpdateRequestDto.class);
        when(maintenanceRegisterService.update(eq(registerId), any())).thenReturn(dto);

        mockMvc.perform(put(ControllerMockMvcSupport.REGISTERS + "/" + registerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(registerId.toString()));

        verify(maintenanceRegisterService).update(eq(registerId), captor.capture());
        assertThat(captor.getValue().getStartDate()).isEqualTo(body.getStartDate());
    }

    @Test
    @DisplayName("PUT update propaga not found")
    void update_cuandoNoExiste_retornaNotFound() throws Exception {
        MaintenanceRegisterUpdateRequestDto body = MaintenanceRegisterUpdateRequestDto.builder()
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build();
        when(maintenanceRegisterService.update(eq(registerId), any()))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(put(ControllerMockMvcSupport.REGISTERS + "/" + registerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }

    @Test
    @DisplayName("POST finalize devuelve registro finalizado")
    void finalizeRegister_cuandoOk_retornaOk() throws Exception {
        MaintenanceRegisterResponseDto dto = MaintenanceRegisterResponseDto.builder().id(registerId).build();
        when(maintenanceRegisterService.finalizeRegister(registerId)).thenReturn(dto);

        mockMvc.perform(post(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/finalize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(registerId.toString()));
    }

    @Test
    @DisplayName("POST finalize propaga error")
    void finalizeRegister_cuandoFalla_retornaNotFound() throws Exception {
        when(maintenanceRegisterService.finalizeRegister(registerId))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/finalize")),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }

    @Test
    @DisplayName("GET assets del registro devuelve página")
    void findRegisterAssets_cuandoHayDatos_retornaOk() throws Exception {
        UUID assetId = UUID.randomUUID();
        MaintenanceAssetOptionDto asset = MaintenanceAssetOptionDto.builder().id(assetId).build();
        when(maintenanceRegisterService.findRegisterAssets(eq(registerId), eq(null), any(), any()))
                .thenReturn(new PageImpl<>(List.of(asset), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(assetId.toString()));
    }

    @Test
    @DisplayName("GET assets propaga error")
    void findRegisterAssets_cuandoFalla_retornaNotFound() throws Exception {
        when(maintenanceRegisterService.findRegisterAssets(eq(registerId), any(), any(), any()))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/assets")),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }

    @Test
    @DisplayName("POST record captura usuario JWT y DTO")
    void createRecord_cuandoPayloadValido_retornaCreated() throws Exception {
        UUID assetId = UUID.randomUUID();
        MaintenanceRecordRequestDto body = MaintenanceRecordRequestDto.builder()
                .assetId(assetId)
                .description("Trabajo realizado")
                .build();
        MaintenanceRecordResponseDto dto = MaintenanceRecordResponseDto.builder().id(UUID.randomUUID()).build();
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        when(maintenanceRecordService.create(eq(registerId), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(post(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(dto.getId().toString()));

        verify(maintenanceRecordService).create(eq(registerId), any(), userCaptor.capture(), emailCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo("kc-user-subject");
        assertThat(emailCaptor.getValue()).isEqualTo("tech@example.com");
    }

    @Test
    @DisplayName("POST record propaga error")
    void createRecord_cuandoFalla_retornaNotFound() throws Exception {
        MaintenanceRecordRequestDto body = MaintenanceRecordRequestDto.builder()
                .assetId(UUID.randomUUID())
                .description("Trabajo")
                .build();
        when(maintenanceRecordService.create(eq(registerId), any(), any(), any()))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }

    @Test
    @DisplayName("GET records del registro devuelve página")
    void findRecords_cuandoHayDatos_retornaOk() throws Exception {
        MaintenanceRecordResponseDto dto = MaintenanceRecordResponseDto.builder().id(UUID.randomUUID()).build();
        when(maintenanceRecordService.findByRegister(eq(registerId), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(dto.getId().toString()));
    }

    @Test
    @DisplayName("GET records propaga error")
    void findRecords_cuandoFalla_retornaNotFound() throws Exception {
        when(maintenanceRecordService.findByRegister(eq(registerId), any(), any()))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.REGISTERS + "/" + registerId + "/records")),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }
}
