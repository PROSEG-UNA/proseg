package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class SupervisorReportValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "SUPERVISOR_REPORT";

    private static final List<FieldSpec> SPECS = List.of(
            text("nombre", "Nombre").required(),
            text("firma", "Firma"),
            date("fecha", "Fecha").required(),
            text("turno", "Turno").required(),
            text("descripcion", "Descripción").required()
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
