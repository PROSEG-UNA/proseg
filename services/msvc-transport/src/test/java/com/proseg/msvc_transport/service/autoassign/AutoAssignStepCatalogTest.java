package com.proseg.msvc_transport.service.autoassign;

import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.draft;
import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.peripheral;
import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.tour;
import static org.assertj.core.api.Assertions.assertThat;

class AutoAssignStepCatalogTest {

    private static final String WEEKDAY = "2026-05-06";
    private static final String SATURDAY = "2026-05-09";

    private final TourProcessingDraft draft = draft();

    private String codeOf(TourProcessingDraftTour tour) {
        return AutoAssignStepCatalog.stepCodeOf(tour, Map.of());
    }

    @Test
    void priorityOneWithFewerThanFivePassengersIsMandatoryEvenOnWeekends() {
        assertThat(codeOf(tour(draft, 1, 1, 4, WEEKDAY))).isEqualTo("MANDATORY");
        assertThat(codeOf(tour(draft, 2, 1, 2, SATURDAY))).isEqualTo("MANDATORY");
        assertThat(codeOf(tour(draft, 3, 1, 5, WEEKDAY))).isEqualTo("PRIORITY_1");
    }

    @Test
    void ulloaToursOfGeneralServicesGoToPeripheral() {
        assertThat(codeOf(peripheral(draft, 1, WEEKDAY))).isEqualTo("PERIPHERAL");
    }

    @Test
    void remainingPriorityToursSplitByWeekendOrExtraHours() {
        assertThat(codeOf(tour(draft, 1, 2, 12, WEEKDAY))).isEqualTo("PRIORITY_2");
        assertThat(codeOf(tour(draft, 2, 2, 12, SATURDAY))).isEqualTo("PRIORITY_2_EXTRA");
        assertThat(codeOf(tour(draft, 3, 1, 12, SATURDAY))).isEqualTo("PRIORITY_1_EXTRA");
    }

    @Test
    void aWeekdayTourWithOvertimeHoursInItsDaysNeedsExtra() {
        TourProcessingDraftTour overtime = tour(draft, 1, 3, 12, WEEKDAY);
        UUID assignmentId = overtime.primaryAssignment().getId();
        TourProcessingDraftDay day = TourProcessingDraftDay.builder()
                .assignment(overtime.primaryAssignment())
                .date(LocalDate.parse(WEEKDAY))
                .ordinaryHours(1.0).surplusHours(2.0).overtimeHours(1.5)
                .build();

        assertThat(AutoAssignStepCatalog.stepCodeOf(overtime, Map.of(assignmentId, List.of(day)))).isEqualTo("PRIORITY_3_EXTRA");
    }

    @Test
    void ordersMandatoryThenPeripheralThenOrdinaryPrioritiesThenExtraPriorities() {
        TreeSet<String> ordered = new TreeSet<>(AutoAssignStepCatalog.STEP_ORDER);
        ordered.addAll(List.of("PRIORITY_2_EXTRA", "PRIORITY_10", "PRIORITY_2", "PERIPHERAL", "PRIORITY_1_EXTRA", "MANDATORY", "PRIORITY_1"));

        assertThat(ordered).containsExactly(
                "MANDATORY", "PERIPHERAL", "PRIORITY_1", "PRIORITY_2", "PRIORITY_10", "PRIORITY_1_EXTRA", "PRIORITY_2_EXTRA");
    }

    @Test
    void labelsEachStepInSpanish() {
        assertThat(AutoAssignStepCatalog.labelOf("MANDATORY")).isEqualTo("Prioridad 1 con menos de 5 pasajeros");
        assertThat(AutoAssignStepCatalog.labelOf("PERIPHERAL")).isEqualTo("Giras a la periférica");
        assertThat(AutoAssignStepCatalog.labelOf("PRIORITY_1")).isEqualTo("Prioridad 1 restantes, sin horas extra");
        assertThat(AutoAssignStepCatalog.labelOf("PRIORITY_4")).isEqualTo("Prioridad 4, sin horas extra");
        assertThat(AutoAssignStepCatalog.labelOf("PRIORITY_4_EXTRA")).isEqualTo("Prioridad 4 con fin de semana u horas extra");
    }

    @Test
    void recognizesOnlyKnownStepCodes() {
        assertThat(AutoAssignStepCatalog.isStepCode("MANDATORY")).isTrue();
        assertThat(AutoAssignStepCatalog.isStepCode("PRIORITY_3_EXTRA")).isTrue();
        assertThat(AutoAssignStepCatalog.isStepCode("PRIORITY_X")).isFalse();
        assertThat(AutoAssignStepCatalog.isStepCode(null)).isFalse();
    }
}
