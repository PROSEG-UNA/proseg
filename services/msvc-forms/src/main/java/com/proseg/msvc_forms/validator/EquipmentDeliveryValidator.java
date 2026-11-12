package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.proseg.msvc_forms.validator.FieldSpec.*;

@Component
public class EquipmentDeliveryValidator extends SpecFormValidator {

    private static final String FORM_TYPE_CODE = "EQUIPMENT_DELIVERY";

    private static final List<FieldSpec> SPECS = List.of(
            date("fecha", "Fecha").required(),
            text("supervisorTurno", "Supervisor de turno").required(),
            text("cedula", "Cédula"),
            object("equipo", "Equipo", List.of(
                    checkItem("radioComunicador", "Radio comunicador"),
                    checkItem("bateriaRadioComunicacion", "Batería radio de comunicación"),
                    checkItem("cargadorBateriasRadioComunicacion", "Cargador baterías radio de comunicación"),
                    checkItem("antenaRadioComunicacion", "Antena radio de comunicación"),
                    checkItem("manosLibres", "Manos libres"),
                    checkItem("paraguas", "Paraguas"),
                    checkItem("esposas", "Esposas"),
                    checkItem("varaPolicial", "Vara policial"),
                    checkItem("controlAgujaParqueos", "Control aguja parqueos"),
                    checkItem("foco", "Foco"),
                    checkItem("bateriaFoco", "Batería foco"),
                    checkItem("cargadorBateriasFoco", "Cargador baterías para foco"),
                    checkItem("armaFuego", "Arma de fuego"),
                    checkItem("gorras", "Gorras"),
                    checkItem("cinturones", "Cinturones"))),
            text("oficialSeguridadQueRecibe", "Oficial de Seguridad que recibe"),
            text("operadorAccesoVehicular", "Operador de acceso vehicular"),
            text("puesto", "Puesto"),
            text("cedulaRecibe", "Cédula"),
            text("firma", "Firma")
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
