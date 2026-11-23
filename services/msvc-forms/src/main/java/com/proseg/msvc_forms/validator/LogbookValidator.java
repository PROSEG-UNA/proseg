package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class LogbookValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "LOGBOOK";

    private static final List<FieldSpec> REGISTRO_SPECS = List.of(
            text("nombre", "Nombre").required(),
            date("fecha", "Fecha").required(),
            time("hora", "Hora"),
            text("puesto", "Puesto"),
            text("jornada", "Jornada"),
            text("grupoNo", "Grupo No."),
            text("supervisor", "Supervisor"),
            object("equipoSeguridad", "Equipo de seguridad", List.of(
                    yesNoItem("armaFuego9mm", "Arma de fuego 9 mm"),
                    yesNoItem("radioComunicacion", "Radio de comunicación"),
                    yesNoItem("foco", "Foco"),
                    yesNoItem("blackJack", "Black Jack"),
                    yesNoItem("bateriasFoco", "Baterías foco"),
                    yesNoItem("cargadorRadio", "Cargador radio"),
                    yesNoItem("varaPolicial", "Vara policial"),
                    yesNoItem("esposas", "Esposas"),
                    yesNoItem("bateriaExtraRadio", "Batería extra radio"))),
            object("equipoVario", "Equipo vario", List.of(
                    yesNoItem("llavesPuesto", "Llaves puesto"),
                    yesNoItem("telefono", "Teléfono"),
                    yesNoItem("extintor", "Extintor"),
                    yesNoItem("pizarra", "Pizarra"),
                    yesNoItem("paraguas", "Paraguas"))),
            object("materialesLimpieza", "Materiales de limpieza", List.of(
                    yesNoItem("papelHigienico", "Papel higiénico"),
                    yesNoItem("lavaplatos", "Lavaplatos"),
                    yesNoItem("jabonPolvo", "Jabón en polvo"),
                    yesNoItem("cloro", "Cloro"),
                    yesNoItem("escoba", "Escoba"),
                    yesNoItem("bolsaGrande", "Bolsa grande"),
                    yesNoItem("isopo", "Isopo"),
                    yesNoItem("limpiones", "Limpiones"),
                    yesNoItem("scottBritte", "Scott Britte"),
                    yesNoItem("jabonLiquido", "Jabón líquido"),
                    yesNoItem("desinfectante", "Desinfectante"),
                    yesNoItem("pala", "Pala"),
                    yesNoItem("bolsaPequena", "Bolsa pequeña"),
                    yesNoItem("paloPiso", "Palo piso"))),
            object("implementosCocina", "Implementos de cocina", List.of(
                    yesNoItem("coffeMaker", "Coffe Maker"),
                    yesNoItem("cuchara", "Cuchara"),
                    yesNoItem("tenedor", "Tenedor"),
                    yesNoItem("microondas", "Microondas"),
                    yesNoItem("tasaCafe", "Tasa café"),
                    yesNoItem("cuchillo", "Cuchillo"),
                    yesNoItem("plato", "Plato"))),
            text("estadoCaseta", "Estado de la caseta"),
            text("descripcionAnomaliasNovedades", "Descripción de anomalías y novedades"));

    private static final List<FieldSpec> SPECS = List.of(
            text("numeroBitacora", "Número de bitácora").required(),
            text("puesto", "Puesto").required(),
            date("vigenciaDesde", "Vigencia desde").required(),
            date("vigenciaHasta", "Vigencia hasta").required(),
            list("registros", "Registros", 1, REGISTRO_SPECS)
    );

    @Override
    protected List<FieldSpec> specs() {
        return SPECS;
    }

    @Override
    protected void validateExtra(JsonNode data) {
        String from = textOf(data, "vigenciaDesde");
        String to = textOf(data, "vigenciaHasta");
        if (from != null && to != null && java.time.LocalDate.parse(to).isBefore(java.time.LocalDate.parse(from))) {
            throw new FormValidationException("La vigencia hasta no puede ser anterior a la vigencia desde");
        }
    }

    @Override
    public String getFormTypeCode() {
        return FORM_TYPE_CODE;
    }
}
