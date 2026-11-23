package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class ShiftChangeValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "SHIFT_CHANGE";

    private static final List<FieldSpec> SPECS = List.of(
            date("fecha", "Fecha").required(),
            text("interesado", "Interesado/a").required(),
            date("interesadoFecha", "Fecha del interesado/a").required(),
            text("interesadoHorario", "Horario del interesado/a").required(),
            text("interesadoPuesto", "Puesto del interesado/a").required(),
            text("interesadoFirma", "Firma del interesado/a"),
            text("sustituye", "Sustituye").required(),
            date("sustituyeFecha", "Fecha de quien sustituye").required(),
            text("sustituyeHorario", "Horario de quien sustituye").required(),
            text("sustituyePuesto", "Puesto de quien sustituye").required(),
            text("sustituyeFirma", "Firma de quien sustituye"),
            text("justificacion", "Justificación").required(),
            choice("estado", "Estado", List.of("AUTORIZADO", "NO_AUTORIZADO")),
            text("firmaJefatura", "Firma Jefatura"),
            text("supervisorInteresado", "Nombre del/de la Supervisor/a del/de la interesado/a"),
            text("supervisorInteresadoFirma", "Firma del/de la Supervisor/a del/de la interesado/a"),
            text("supervisorSustituye", "Nombre del/de la Supervisor/a del/de la que sustituye"),
            text("supervisorSustituyeFirma", "Firma del/de la Supervisor/a del/de la que sustituye")
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
