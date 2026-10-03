package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class PostAnomaliesValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "POST_ANOMALIES";

    private static final List<FieldSpec> SPECS = List.of(
            text("oficialSeguridadQueReporta", "Oficial de seguridad que reporta").required(),
            date("fecha", "Fecha").required(),
            text("turno", "Turno").required(),
            text("nombrePuesto", "Nombre del puesto").required(),
            text("supervisorResponsable", "Supervisor/a responsable").required(),
            text("observaciones", "Observaciones").required()
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
