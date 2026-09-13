package com.proseg.msvc_forms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_forms.dto.request.FormRecordCreateRequestDto;
import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import com.proseg.msvc_forms.dto.response.FormTypeResponseDto;
import com.proseg.msvc_forms.exception.GlobalExceptionHandler;
import com.proseg.msvc_forms.service.FormRecordService;
import com.proseg.msvc_forms.service.FormTypeService;
import com.proseg.msvc_forms.validator.FormValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FormControllersTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FormTypeService formTypeService = mock(FormTypeService.class);
    private final FormRecordService formRecordService = mock(FormRecordService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new FormTypeController(formTypeService), new FormRecordController(formRecordService))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getTypes_should_return_ok() throws Exception {
        when(formTypeService.getAllActiveTypes()).thenReturn(List.of(FormTypeResponseDto.builder().id(UUID.randomUUID()).code("OVERTIME_REPORT").name("Reporte de Horas Extras").build()));

        mockMvc.perform(get("/api/v1/forms/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getTypeByCode_should_return_ok() throws Exception {
        when(formTypeService.getTypeByCode("OVERTIME_REPORT")).thenReturn(FormTypeResponseDto.builder().code("OVERTIME_REPORT").name("Reporte de Horas Extras").build());

        mockMvc.perform(get("/api/v1/forms/types/code/OVERTIME_REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("OVERTIME_REPORT"));
    }

    @Test
    void getForms_should_return_ok() throws Exception {
        when(formRecordService.findAll(isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of(FormRecordResponseDto.builder().id(UUID.randomUUID()).build()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_should_return_ok() throws Exception {
        UUID id = UUID.randomUUID();
        when(formRecordService.getById(id)).thenReturn(FormRecordResponseDto.builder().id(id).build());

        mockMvc.perform(get("/api/v1/forms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void post_should_return_created() throws Exception {
        UUID typeId = UUID.randomUUID();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(typeId, objectMapper.valueToTree(java.util.Map.of("fecha", "2026-09-13")));
        when(formRecordService.create(any(FormRecordCreateRequestDto.class), any(Authentication.class)))
                .thenReturn(FormRecordResponseDto.builder().id(UUID.randomUUID()).formTypeId(typeId).build());

        mockMvc.perform(post("/api/v1/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void put_should_return_ok() throws Exception {
        UUID id = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(typeId, objectMapper.valueToTree(java.util.Map.of("fecha", "2026-09-13")));
        when(formRecordService.update(eq(id), any(FormRecordCreateRequestDto.class), any(Authentication.class)))
                .thenReturn(FormRecordResponseDto.builder().id(id).formTypeId(typeId).build());

        mockMvc.perform(put("/api/v1/forms/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_should_return_ok() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/forms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void post_should_return_400_when_validation_fails() throws Exception {
        mockMvc.perform(post("/api/v1/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_should_return_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(formRecordService.getById(id)).thenThrow(new ResponseStatusException(NOT_FOUND, "No existe"));

        mockMvc.perform(get("/api/v1/forms/{id}", id))
                .andExpect(status().isNotFound());
    }
}
