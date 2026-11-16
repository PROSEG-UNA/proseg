package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingAutoAssignWizardDto;
import com.proseg.msvc_transport.service.autoassign.TourProcessingAutoAssignWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${routes.tour-processing:/api/v1/transport/tour-processing}/drafts/{id}/auto-assign-wizard")
@RequiredArgsConstructor
public class TourProcessingAutoAssignController {

    private final TourProcessingAutoAssignWizardService wizardService;

    @GetMapping
    public ResponseEntity<ApiResponse<TourProcessingAutoAssignWizardDto>> state(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(wizardService.state(id), "Estado del asistente de autoasignación");
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<TourProcessingAutoAssignWizardDto>> start(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(wizardService.start(id), "Asistente de autoasignación iniciado");
    }

    @PostMapping("/recalculate")
    public ResponseEntity<ApiResponse<TourProcessingAutoAssignWizardDto>> recalculate(@PathVariable UUID id) {
        return ApiResponseBuilder.ok(wizardService.recalculate(id), "Autoasignación recalculada");
    }

    @PutMapping("/progress")
    public ResponseEntity<ApiResponse<TourProcessingAutoAssignWizardDto>> setStep(
            @PathVariable UUID id,
            @RequestBody TourProcessingAutoAssignWizardDto.ProgressRequest request
    ) {
        return ApiResponseBuilder.ok(wizardService.setStep(id, request.currentStep()), "Paso actual guardado");
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<TourProcessingAutoAssignWizardDto>> save(
            @PathVariable UUID id,
            @RequestBody TourProcessingAutoAssignWizardDto.ProgressRequest request
    ) {
        return ApiResponseBuilder.ok(wizardService.save(id, request.currentStep()), "Progreso guardado");
    }

    @PostMapping("/finish")
    public ResponseEntity<ApiResponse<Void>> finish(@PathVariable UUID id) {
        wizardService.finish(id);
        return ApiResponseBuilder.ok(null, "Autoasignación terminada");
    }

    @PostMapping("/discard")
    public ResponseEntity<ApiResponse<Void>> discard(@PathVariable UUID id) {
        wizardService.discard(id);
        return ApiResponseBuilder.ok(null, "Cambios del asistente descartados");
    }
}
