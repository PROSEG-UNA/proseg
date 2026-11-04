package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.UlloaShift;
import org.junit.jupiter.api.Test;

import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.sameDayTour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.tour;
import static com.proseg.msvc_transport.service.driverhours.DriverHoursTestRows.ulloaTour;
import static org.assertj.core.api.Assertions.assertThat;

class UlloaToursTest {

    @Test
    void recognizesEachOfTheThreeShifts() {
        assertThat(UlloaTours.shiftOf(ulloaTour("1", "2026-05-05", UlloaShift.SHORT_MORNING))).contains(UlloaShift.SHORT_MORNING);
        assertThat(UlloaTours.shiftOf(ulloaTour("2", "2026-05-05", UlloaShift.MORNING))).contains(UlloaShift.MORNING);
        assertThat(UlloaTours.shiftOf(ulloaTour("3", "2026-05-05", UlloaShift.AFTERNOON))).contains(UlloaShift.AFTERNOON);
    }

    @Test
    void ignoresAccentsCaseAndExtraSpacesInUnitAndDestination() {
        TourProcessingDraftAssignment tour = ulloaTour("1", "2026-05-05", UlloaShift.MORNING);
        tour.getTour().setExecutingUnit("  PROGRAMA   de servicios GENERALES ");
        tour.getTour().setDestination("Campus Benjamín Núñez - ULLOA (Barreal)");

        assertThat(UlloaTours.isUlloaTour(tour)).isTrue();
    }

    @Test
    void rejectsOtherUnitsOtherDestinationsAndOtherSchedules() {
        TourProcessingDraftAssignment otherUnit = ulloaTour("1", "2026-05-05", UlloaShift.MORNING);
        otherUnit.getTour().setExecutingUnit("Escuela De Ciencias Ambientales");
        TourProcessingDraftAssignment otherDestination = ulloaTour("2", "2026-05-05", UlloaShift.MORNING);
        otherDestination.getTour().setDestination("Heredia");
        TourProcessingDraftAssignment otherSchedule = sameDayTour("3", "2026-05-05", "08:00", "12:00");
        otherSchedule.getTour().setExecutingUnit("Programa De Servicios Generales");
        otherSchedule.getTour().setDestination("Ulloa (barreal)");

        assertThat(UlloaTours.isUlloaTour(otherUnit)).isFalse();
        assertThat(UlloaTours.isUlloaTour(otherDestination)).isFalse();
        assertThat(UlloaTours.isUlloaTour(otherSchedule)).isFalse();
    }

    @Test
    void rejectsToursThatSpanMoreThanOneDay() {
        TourProcessingDraftAssignment multiDay = tour("1", "2026-05-05", "06:30", "2026-05-06", "17:30");
        multiDay.getTour().setExecutingUnit("Programa De Servicios Generales");
        multiDay.getTour().setDestination("Ulloa (barreal)");

        assertThat(UlloaTours.isUlloaTour(multiDay)).isFalse();
    }

    @Test
    void toleratesToursWithoutDestinationOrUnit() {
        TourProcessingDraftAssignment withoutData = sameDayTour("1", "2026-05-05", "06:30", "17:30");
        withoutData.getTour().setExecutingUnit(null);
        withoutData.getTour().setDestination(null);

        assertThat(UlloaTours.isUlloaTour(withoutData)).isFalse();
    }
}
