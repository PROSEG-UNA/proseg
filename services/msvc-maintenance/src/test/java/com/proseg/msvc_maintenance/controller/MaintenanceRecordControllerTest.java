package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.proseg.msvc_maintenance.exception.MaintenanceRegisterException;
import com.proseg.msvc_maintenance.service.MaintenanceRecordService;
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
class MaintenanceRecordControllerTest {

    @Mock
    private MaintenanceRecordService maintenanceRecordService;

    private MockMvc mockMvc;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new MaintenanceRecordController(maintenanceRecordService),
                "routes.records",
                ControllerMockMvcSupport.RECORDS);
        assetId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET records por activo responde 200 con página")
    void findByAsset_cuandoHayDatos_retornaOk() throws Exception {
        MaintenanceRecordResponseDto dto = MaintenanceRecordResponseDto.builder()
                .id(UUID.randomUUID())
                .build();
        when(maintenanceRecordService.findByAsset(eq(assetId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.RECORDS)
                        .param("assetId", assetId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(dto.getId().toString()));
    }

    @Test
    @DisplayName("GET records propaga excepción del servicio")
    void findByAsset_cuandoServicioFalla_retornaNotFound() throws Exception {
        when(maintenanceRecordService.findByAsset(eq(assetId), any()))
                .thenThrow(MaintenanceRegisterException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.RECORDS)
                        .param("assetId", assetId.toString())),
                404,
                "MAINTENANCE_REGISTER_NOT_FOUND");
    }
}
