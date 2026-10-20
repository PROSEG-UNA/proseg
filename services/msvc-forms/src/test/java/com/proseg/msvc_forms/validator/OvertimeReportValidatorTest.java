package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OvertimeReportValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private OvertimeReportValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OvertimeReportValidator();
    }

    @Test
    void validate_should_accept_one_valid_row() {
        assertThatCode(() -> validator.validate(payload(List.of(validRow(1, "17:00", "19:00", 2.0)))))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_should_accept_multiple_rows() {
        assertThatCode(() -> validator.validate(payload(List.of(
                validRow(1, "17:00", "19:00", 2.0),
                validRow(2, "19:00", "22:00", 3.0)
        )))).doesNotThrowAnyException();
    }

    @Test
    void validate_should_accept_matching_total_hours() {
        assertThatCode(() -> validator.validate(payload(List.of(validRow(1, "17:15", "19:45", 2.5)))))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_should_reject_missing_required_fields() {
        JsonNode payload = objectMapper.valueToTree(Map.of(
                "fecha", "2026-09-13",
                "grupo", "",
                "supervisor", "Supervisor",
                "detalle", List.of(validRow(1, "17:00", "19:00", 2.0))
        ));

        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(FormValidationException.class);
    }

    @Test
    void validate_should_reject_empty_detail() {
        JsonNode payload = objectMapper.valueToTree(Map.of(
                "fecha", "2026-09-13",
                "grupo", "Grupo A",
                "supervisor", "Supervisor",
                "detalle", List.of()
        ));

        assertThatThrownBy(() -> validator.validate(payload))
                .isInstanceOf(FormValidationException.class)
                .hasMessageContaining("al menos una fila");
    }

    @Test
    void validate_should_reject_invalid_schedule() {
        assertThatThrownBy(() -> validator.validate(payload(List.of(validRow(1, "19:00", "17:00", 2.0)))))
                .isInstanceOf(FormValidationException.class)
                .hasMessageContaining("hora de salida");
    }

    private JsonNode payload(List<Map<String, Object>> details) {
        return objectMapper.valueToTree(Map.of(
                "fecha", "2026-09-13",
                "grupo", "Grupo A",
                "supervisor", "Supervisor Uno",
                "detalle", details,
                "observacion", "Obs"
        ));
    }

    private Map<String, Object> validRow(int number, String entry, String exit, double total) {
        return Map.of(
                "no", number,
                "nombre", "Persona " + number,
                "cedula", "1-1111-111" + number,
                "fecha", "2026-09-13",
                "horaEntrada", entry,
                "horaSalida", exit,
                "totalHoras", total
        );
    }
}
