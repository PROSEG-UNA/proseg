package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtendedFormValidatorsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) throws Exception {
        return mapper.readTree(raw);
    }

    @Test
    void codes_are_unique_and_stable() {
        assertThat(new EquipmentDeliveryValidator().getFormTypeCode()).isEqualTo("EQUIPMENT_DELIVERY");
        assertThat(new LogbookValidator().getFormTypeCode()).isEqualTo("LOGBOOK");
    }

    @Test
    void equipment_delivery_accepts_valid_and_rejects_missing_date() throws Exception {
        EquipmentDeliveryValidator v = new EquipmentDeliveryValidator();
        assertThatCode(() -> v.validate(json("""
                {"fecha":"2026-10-01","supervisorTurno":"Ana",
                 "equipo":{"radioComunicador":{"entregado":true,"descripcion":"Motorola"},"paraguas":{"entregado":false}}}
                """))).doesNotThrowAnyException();
        assertThatThrownBy(() -> v.validate(json("{\"supervisorTurno\":\"Ana\"}")))
                .isInstanceOf(FormValidationException.class).hasMessageContaining("Fecha");
    }

    @Test
    void shift_change_rejects_invalid_estado() {
        ShiftChangeValidator v = new ShiftChangeValidator();
        assertThatThrownBy(() -> v.validate(json("""
                {"fecha":"2026-10-01","interesado":"A","interesadoFecha":"2026-10-02","interesadoHorario":"6-2",
                 "interesadoPuesto":"P","sustituye":"B","sustituyeFecha":"2026-10-02","sustituyeHorario":"6-2",
                 "sustituyePuesto":"P","justificacion":"x","estado":"AMBOS"}
                """))).isInstanceOf(FormValidationException.class);
    }

    @Test
    void vacation_requires_otro_description_and_date_order() {
        VacationPermitRequestValidator v = new VacationPermitRequestValidator();
        String base = "{\"nombreSolicitante\":\"A\",\"cedula\":\"1\",\"tramiteSolicitado\":\"%s\","
                + "\"fechaDesde\":\"%s\",\"fechaHasta\":\"%s\",\"cantidadDias\":2}";
        assertThatThrownBy(() -> v.validate(json(base.formatted("OTRO", "2026-10-01", "2026-10-02"))))
                .hasMessageContaining("Otro");
        assertThatThrownBy(() -> v.validate(json(base.formatted("VACACIONES", "2026-10-05", "2026-10-02"))))
                .hasMessageContaining("fecha hasta");
        assertThatCode(() -> v.validate(json(base.formatted("VACACIONES", "2026-10-01", "2026-10-02"))))
                .doesNotThrowAnyException();
    }

    @Test
    void repeatable_forms_require_rows_and_validate_each_row() {
        ServiceRosterValidator roster = new ServiceRosterValidator();
        String head = "\"grupo\":\"1\",\"turnoServicioDesde\":\"06:00\",\"turnoServicioHasta\":\"14:00\","
                + "\"fecha\":\"2026-10-01\",\"nombreSupervisor\":\"S\"";
        assertThatThrownBy(() -> roster.validate(json("{" + head + ",\"detalle\":[]}")))
                .hasMessageContaining("al menos");
        assertThatThrownBy(() -> roster.validate(json("{" + head + ",\"detalle\":[{\"numero\":1}]}")))
                .hasMessageContaining("Fila 1");
        assertThatCode(() -> roster.validate(json("{" + head
                + ",\"detalle\":[{\"numero\":1,\"puestoAsignado\":\"Deportes\"},{\"numero\":2,\"puestoAsignado\":\"CINPE\"}]}")))
                .doesNotThrowAnyException();

        AccessControlValidator access = new AccessControlValidator();
        assertThatThrownBy(() -> access.validate(json("""
                {"fecha":"2026-10-01","puesto":"P","detalle":[
                 {"nombrePersona":"X","numeroCedula":"1","numeroTelefono":"abc"}]}
                """))).hasMessageContaining("teléfono");
    }

    @Test
    void logbook_validates_nested_registros() {
        LogbookValidator v = new LogbookValidator();
        String head = "\"numeroBitacora\":\"10\",\"puesto\":\"P\",\"vigenciaDesde\":\"2026-10-01\",\"vigenciaHasta\":\"2026-12-31\"";
        assertThatCode(() -> v.validate(json("{" + head + ",\"registros\":[{\"nombre\":\"N\",\"fecha\":\"2026-10-01\","
                + "\"equipoVario\":{\"llavesPuesto\":{\"valor\":\"SI\",\"cantidad\":2}}}]}")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> v.validate(json("{" + head + ",\"registros\":[{\"nombre\":\"N\",\"fecha\":\"2026-10-01\","
                + "\"equipoSeguridad\":{\"foco\":{\"valor\":\"TAL VEZ\"}}}]}")))
                .isInstanceOf(FormValidationException.class);
    }
}
