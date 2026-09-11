package com.proseg.msvcinventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvcinventory.dto.request.AssetRequestDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.service.AssetService;
import com.proseg.msvcinventory.controller.AssetController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class AssetControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("POST /assets crea activo y responde 201 con body")
    void postAssets_creaActivo_responde201() throws Exception {
        AssetService service = Mockito.mock(AssetService.class);
        // controller constructor expects (AssetService assetService, AssetArchiveService assetArchiveService, AssetImportService assetImportService)
        AssetController controller = new AssetController(service, null, null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        AssetRequestDto req = new AssetRequestDto();
        req.setModelId(UUID.randomUUID());
        req.setLocationId(UUID.randomUUID());
        req.setStatus(com.proseg.msvcinventory.entity.enums.AssetStatus.APROBADO);
        req.setAssetNumber("ACT-001");
        req.setSerialNumber("SER-001");
        req.setLatitude(new BigDecimal("9.99"));
        req.setLongitude(new BigDecimal("-84.01"));

        AssetResponseDto dto = new AssetResponseDto();
        dto.setId(UUID.randomUUID());

        when(service.create(any())).thenReturn(dto);

        mvc.perform(post("/api/v1/inventory/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }
}
