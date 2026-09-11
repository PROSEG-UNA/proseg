package com.proseg.msvcinventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvcinventory.controller.AssetController;
import com.proseg.msvcinventory.controller.BrandController;
import com.proseg.msvcinventory.dto.request.AssetRequestDto;
import com.proseg.msvcinventory.dto.request.BrandRequestDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.dto.response.BrandResponseDto;
import com.proseg.msvcinventory.service.AssetArchiveService;
import com.proseg.msvcinventory.service.AssetImportService;
import com.proseg.msvcinventory.service.AssetService;
import com.proseg.msvcinventory.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllerCoverageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void assetController_postCreate_returnsCreated() throws Exception {
        AssetService assetService = mock(AssetService.class);
        AssetArchiveService assetArchiveService = mock(AssetArchiveService.class);
        AssetImportService assetImportService = mock(AssetImportService.class);
        AssetController controller = new AssetController(assetService, assetArchiveService, assetImportService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        AssetRequestDto request = new AssetRequestDto();
        request.setAssetNumber("ACT-101");
        request.setStatus(com.proseg.msvcinventory.entity.enums.AssetStatus.APROBADO);
        request.setSerialNumber("SER-101");
        request.setModelId(UUID.randomUUID());
        request.setLocationId(UUID.randomUUID());
        request.setLatitude(new BigDecimal("9.99"));
        request.setLongitude(new BigDecimal("-84.01"));

        AssetResponseDto dto = new AssetResponseDto();
        dto.setId(UUID.randomUUID());

        when(assetService.create(any())).thenReturn(dto);

        mvc.perform(post("/api/v1/inventory/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void brandController_getList_returnsOk() throws Exception {
        BrandService brandService = mock(BrandService.class);
        BrandController controller = new BrandController(brandService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        BrandResponseDto dto = new BrandResponseDto();
        dto.setId(UUID.randomUUID());
        dto.setName("Dell");

        when(brandService.findAll(eq(null), any(Map.class), any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mvc.perform(get("/api/v1/inventory/brands").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Dell"));
    }
}
