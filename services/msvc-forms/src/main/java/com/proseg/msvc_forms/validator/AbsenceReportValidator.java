package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_forms.dto.request.AbsenceReportData;
import org.springframework.stereotype.Component;

@Component
public class AbsenceReportValidator implements FormValidator {

    private static final String FORM_TYPE_CODE = "ABSENCE_REPORT";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void validate(JsonNode data) throws FormValidationException {
        try {
            AbsenceReportData reportData = objectMapper.treeToValue(data, AbsenceReportData.class);
            validateAbsenceReport(reportData);
        } catch (FormValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormValidationException("INVALID_JSON", 
                "El formato del formulario de ausencia no es válido: " + ex.getMessage());
        }
    }

    private void validateAbsenceReport(AbsenceReportData data) {
        if (data.getDate() == null) {
            throw new FormValidationException("La fecha es obligatoria");
        }
        if (data.getTime() == null) {
            throw new FormValidationException("La hora es obligatoria");
        }
        if (data.getGuard() == null || data.getGuard().isBlank()) {
            throw new FormValidationException("El guarda es obligatorio");
        }
        if (data.getShift() == null || data.getShift().isBlank()) {
            throw new FormValidationException("El turno es obligatorio");
        }
        if (data.getShiftTime() == null || data.getShiftTime().isBlank()) {
            throw new FormValidationException("La hora del turno es obligatoria");
        }
        if (data.getWorkPosition() == null || data.getWorkPosition().isBlank()) {
            throw new FormValidationException("El puesto de trabajo es obligatorio");
        }
        if (data.getAbsenceReason() == null || data.getAbsenceReason().isBlank()) {
            throw new FormValidationException("El motivo de ausencia es obligatorio");
        }
        if (data.getSupervisor() == null || data.getSupervisor().isBlank()) {
            throw new FormValidationException("El supervisor es obligatorio");
        }
    }

    @Override
    public String getFormTypeCode() {
        return FORM_TYPE_CODE;
    }
}
