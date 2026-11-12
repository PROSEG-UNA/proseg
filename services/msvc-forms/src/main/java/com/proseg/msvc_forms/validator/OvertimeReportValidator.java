package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_forms.dto.request.OvertimeReportData;
import com.proseg.msvc_forms.dto.request.OvertimeReportDetailRow;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class OvertimeReportValidator implements FormValidator {

    private static final String FORM_TYPE_CODE = "OVERTIME_REPORT";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void validate(JsonNode data) throws FormValidationException {
        try {
            OvertimeReportData reportData = objectMapper.treeToValue(data, OvertimeReportData.class);
            validateOvertimeReport(reportData);
        } catch (FormValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormValidationException("INVALID_JSON", 
                "El formato del formulario de horas extras no es válido: " + ex.getMessage());
        }
    }

    private void validateOvertimeReport(OvertimeReportData data) {
        if (data.getDate() == null) {
            throw new FormValidationException("La fecha es obligatoria");
        }
        if (data.getGroup() == null || data.getGroup().isBlank()) {
            throw new FormValidationException("El grupo es obligatorio");
        }
        if (data.getSupervisor() == null || data.getSupervisor().isBlank()) {
            throw new FormValidationException("El supervisor es obligatorio");
        }
        if (data.getDetails() == null || data.getDetails().isEmpty()) {
            throw new FormValidationException("Debe existir al menos una fila de detalle");
        }

        validateDetailRows(data.getDetails());
    }

    private void validateDetailRows(List<OvertimeReportDetailRow> details) {
        for (int i = 0; i < details.size(); i++) {
            OvertimeReportDetailRow row = details.get(i);
            
            if (row.getNumber() == null || row.getNumber() <= 0) {
                throw new FormValidationException("Fila " + (i + 1) + ": El número debe ser mayor a 0");
            }
            if (row.getName() == null || row.getName().isBlank()) {
                throw new FormValidationException("Fila " + (i + 1) + ": El nombre es obligatorio");
            }
            if (row.getCedula() == null || row.getCedula().isBlank()) {
                throw new FormValidationException("Fila " + (i + 1) + ": La cédula es obligatoria");
            }
            if (row.getDate() == null) {
                throw new FormValidationException("Fila " + (i + 1) + ": La fecha es obligatoria");
            }
            if (row.getEntryTime() == null) {
                throw new FormValidationException("Fila " + (i + 1) + ": La hora de entrada es obligatoria");
            }
            if (row.getExitTime() == null) {
                throw new FormValidationException("Fila " + (i + 1) + ": La hora de salida es obligatoria");
            }
            
            Duration duration = validateTimeRange(row, i);
            validateCalculatedHours(row, i, duration);
        }
    }

    private Duration validateTimeRange(OvertimeReportDetailRow row, int rowIndex) {
        LocalTime entryTime = row.getEntryTime();
        LocalTime exitTime = row.getExitTime();

        if (exitTime.equals(entryTime)) {
            throw new FormValidationException("Fila " + (rowIndex + 1) + 
                ": La hora de salida debe ser distinta de la hora de entrada");
        }

        LocalDate exitDate = exitTime.isBefore(entryTime)
                ? row.getDate().plusDays(1)
                : row.getDate();
        LocalDateTime entryDateTime = LocalDateTime.of(row.getDate(), entryTime);
        LocalDateTime exitDateTime = LocalDateTime.of(exitDate, exitTime);
        Duration duration = Duration.between(entryDateTime, exitDateTime);

        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofHours(24)) > 0) {
            throw new FormValidationException("Fila " + (rowIndex + 1) +
                ": La duración del turno debe ser mayor a 0 y no exceder 24 horas");
        }

        return duration;
    }

    private void validateCalculatedHours(OvertimeReportDetailRow row, int rowIndex, Duration duration) {
        if (row.getTotalHours() != null && row.getTotalHours() < 0) {
            throw new FormValidationException("Fila " + (rowIndex + 1) + 
                ": El total de horas no puede ser negativo");
        }
        
        double calculatedHours = duration.toMinutes() / 60.0;
        
        if (row.getTotalHours() != null && Math.abs(row.getTotalHours() - calculatedHours) > 0.01) {
            throw new FormValidationException("Fila " + (rowIndex + 1) + 
                ": El total de horas no coincide con la diferencia entre entrada y salida");
        }
    }

    @Override
    public String getFormTypeCode() {
        return FORM_TYPE_CODE;
    }
}
