package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class DailyRoundsControlValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "DAILY_ROUNDS_CONTROL";

    private static final List<FieldSpec> SPECS = List.of(
            text("nombreSupervisorTurno", "Nombre del supervisor de turno").required(),
            text("firma", "Firma"),
            time("turnoDesde", "Turno de las").required(),
            time("turnoHasta", "a las").required(),
            text("grupo", "Grupo").required(),
            date("fecha", "Fecha").required(),
            list("detalle", "Detalle", 1, List.of(
                    text("puesto", "Puesto").required(),
                    time("hora", "Hora").required(),
                    text("nombreOficialSeguridad", "Nombre oficial de seguridad").required(),
                    text("firmaOficialSeguridad", "Firma oficial de seguridad")))
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
