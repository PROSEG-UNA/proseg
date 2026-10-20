package com.proseg.msvc_forms.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_forms.dto.response.FormTypeResponseDto;
import com.proseg.msvc_forms.service.FormTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${routes.forms:/api/v1/forms}")
@RequiredArgsConstructor
public class FormTypeController {

    private final FormTypeService formTypeService;

    @GetMapping("/types")
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).LEER)")
    public ResponseEntity<ApiResponse<List<FormTypeResponseDto>>> getActiveTypes() {
        return ApiResponseBuilder.ok(
                formTypeService.getAllActiveTypes(),
                "Tipos de formularios disponibles"
        );
    }

    @GetMapping("/types/{id}")
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).LEER)")
    public ResponseEntity<ApiResponse<FormTypeResponseDto>> getTypeById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                formTypeService.getTypeById(id),
                "Tipo de formulario obtenido correctamente"
        );
    }

    @GetMapping("/types/code/{code}")
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).LEER)")
    public ResponseEntity<ApiResponse<FormTypeResponseDto>> getTypeByCode(@PathVariable String code) {
        return ApiResponseBuilder.ok(
                formTypeService.getTypeByCode(code),
                "Tipo de formulario obtenido correctamente"
        );
    }
}
