package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class VacationPermitRequestValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "VACATION_PERMIT_REQUEST";

    private static final List<FieldSpec> SPECS = List.of(
            text("nombreSolicitante", "Nombre del/de la solicitante").required(),
            text("cedula", "Cédula #").required(),
            text("grupo", "Grupo #"),
            choice("tramiteSolicitado", "Trámite solicitado", List.of("VACACIONES", "PERMISO", "OTRO")).required(),
            text("otroEspecifique", "Otro (especifique)"),
            date("fechaDesde", "Fecha desde").required(),
            date("fechaHasta", "Fecha hasta").required(),
            date("fechaRegreso", "Fecha de regreso"),
            integer("cantidadDias", "Cantidad de días solicitados", 1).required(),
            text("observaciones", "Observaciones"),
            date("fechaSolicitud", "Fecha cuando realiza la solicitud"),
            text("firmaSolicitante", "Firma del/de la solicitante"),
            date("fechaRecibido", "Fecha de recibido"),
            time("horaRecibido", "Hora de recibido"),
            text("explicacionNoAutoriza", "Explicación de por qué no se autoriza la solicitud"),
            choice("autorizado", "Autorizado", FieldSpec.YES_NO),
            text("firmaSupervisor", "Firma del/de la supervisor/a"),
            text("secretariaNombreSolicitante", "Nombre del/de la solicitante (secretaría)"),
            text("secretariaFechasSolicitadas", "Fechas solicitadas"),
            text("secretariaFirmaHoraFechaRecibido", "Firma, hora y fecha de recibido")
    );

    @Override
    protected List<FieldSpec> specs() {
        return SPECS;
    }

    @Override
    protected void validateExtra(JsonNode data) {
        if ("OTRO".equals(textOf(data, "tramiteSolicitado")) && textOf(data, "otroEspecifique") == null) {
            throw new FormValidationException("Debe especificar el trámite cuando selecciona Otro");
        }
        String from = textOf(data, "fechaDesde");
        String to = textOf(data, "fechaHasta");
        if (from != null && to != null && java.time.LocalDate.parse(to).isBefore(java.time.LocalDate.parse(from))) {
            throw new FormValidationException("La fecha hasta no puede ser anterior a la fecha desde");
        }
    }

    @Override
    public String getFormTypeCode() {
        return FORM_TYPE_CODE;
    }
}
