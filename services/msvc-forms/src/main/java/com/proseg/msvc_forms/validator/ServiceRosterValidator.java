package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class ServiceRosterValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "SERVICE_ROSTER";

    private static final List<FieldSpec> SPECS = List.of(
            text("grupo", "Grupo").required(),
            time("turnoServicioDesde", "Turno de servicio desde").required(),
            time("turnoServicioHasta", "Turno de servicio hasta").required(),
            date("fecha", "Fecha").required(),
            text("nombreSupervisor", "Nombre del/de la supervisor/a").required(),
            text("firma", "Firma"),
            list("detalle", "Detalle", 1, List.of(
                    integer("numero", "N.°", 1).required(),
                    text("puestoAsignado", "Puesto asignado").required(),
                    text("nombreOficialSeguridad", "Nombre del/de la oficial de seguridad"),
                    text("motivo", "Motivo"))),
            text("observaciones", "Observaciones")
    );

    @Override
    protected List<FieldSpec> specs() {
        return SPECS;
    }

    @Override
    public String getFormTypeCode() {
        return FORM_TYPE_CODE;
    }
}
