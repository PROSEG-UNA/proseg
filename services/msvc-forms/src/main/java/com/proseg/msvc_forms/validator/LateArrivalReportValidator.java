package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_forms.dto.request.LateArrivalReportData;
import org.springframework.stereotype.Component;

@Component
public class LateArrivalReportValidator implements FormValidator {

    private static final String FORM_TYPE_CODE = "LATE_ARRIVAL_REPORT";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void validate(JsonNode data) throws FormValidationException {
        try {
            LateArrivalReportData reportData = objectMapper.treeToValue(data, LateArrivalReportData.class);
            validateLateArrivalReport(reportData);
        } catch (FormValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormValidationException("INVALID_JSON", 
                "El formato del formulario de llegada tardía no es válido: " + ex.getMessage());
        }
    }

    private void validateLateArrivalReport(LateArrivalReportData data) {
        if (data.getDate() == null) {
            throw new FormValidationException("La fecha es obligatoria");
        }
        if (data.getSecurityOfficer() == null || data.getSecurityOfficer().isBlank()) {
            throw new FormValidationException("El oficial de seguridad es obligatorio");
        }
        if (data.getVehicleAccessOperator() == null || data.getVehicleAccessOperator().isBlank()) {
            throw new FormValidationException("El operador de acceso vehicular es obligatorio");
        }
        if (data.getArrivalTime() == null) {
            throw new FormValidationException("La hora de llegada tardía es obligatoria");
        }
        if (data.getPosition() == null || data.getPosition().isBlank()) {
            throw new FormValidationException("El puesto es obligatorio");
        }
        if (data.getReason() == null || data.getReason().isBlank()) {
            throw new FormValidationException("El motivo de la llegada tardía es obligatorio");
        }
        if (data.getSupervisor() == null || data.getSupervisor().isBlank()) {
            throw new FormValidationException("El supervisor/a de turno es obligatorio");
        }
    }

    @Override
    public String getFormTypeCode() {
        return FORM_TYPE_CODE;
    }
}
