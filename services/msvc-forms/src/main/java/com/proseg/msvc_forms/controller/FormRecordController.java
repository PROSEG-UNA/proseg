package com.proseg.msvc_forms.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.common.api.util.PageMapper;
import com.proseg.msvc_forms.dto.request.FormRecordCreateRequestDto;
import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import com.proseg.msvc_forms.security.Privileges;
import com.proseg.msvc_forms.service.FormRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${routes.forms:/api/v1/forms}")
@RequiredArgsConstructor
public class FormRecordController {

    private final FormRecordService formRecordService;

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).CREAR)")
    public ResponseEntity<ApiResponse<FormRecordResponseDto>> create(
            @Valid @RequestBody FormRecordCreateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.created(
                formRecordService.create(request, authentication),
                "Formulario registrado correctamente"
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).LEER)")
    public ResponseEntity<ApiResponse<PageResponse<FormRecordResponseDto>>> findAll(
            @RequestParam(required = false) UUID formTypeId,
            @RequestParam(required = false) String createdBy,
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                PageMapper.from(formRecordService.findAll(formTypeId, createdBy, pageable, authentication)),
                "Lista de formularios"
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).LEER)")
    public ResponseEntity<ApiResponse<FormRecordResponseDto>> getById(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(
                formRecordService.getById(id),
                "Formulario obtenido correctamente"
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).EDITAR)")
    public ResponseEntity<ApiResponse<FormRecordResponseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody FormRecordCreateRequestDto request,
            Authentication authentication) {
        return ApiResponseBuilder.ok(
                formRecordService.update(id, request, authentication),
                "Formulario actualizado correctamente"
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.proseg.msvc_forms.security.Privileges.Formularios).ELIMINAR)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        formRecordService.delete(id);
        return ApiResponseBuilder.noContent("Formulario eliminado correctamente");
    }
}
