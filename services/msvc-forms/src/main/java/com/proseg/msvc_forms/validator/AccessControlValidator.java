package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class AccessControlValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "ACCESS_CONTROL";

    private static final List<FieldSpec> SPECS = List.of(
            date("fecha", "Fecha").required(),
            text("puesto", "Puesto").required(),
            list("detalle", "Detalle", 1, List.of(
                    text("numeroPlaca", "N° placa"),
                    text("nombrePersona", "Nombre de la persona").required(),
                    text("numeroCedula", "N° cédula").required(),
                    text("oficinaVisitar", "Oficina a visitar"),
                    phone("numeroTelefono", "Número teléfono")))
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
