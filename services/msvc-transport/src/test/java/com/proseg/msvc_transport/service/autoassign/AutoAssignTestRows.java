package com.proseg.msvc_transport.service.autoassign;

import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

final class AutoAssignTestRows {

    private AutoAssignTestRows() {
    }

    static TourProcessingDraft draft() {
        return TourProcessingDraft.builder().id(UUID.randomUUID()).status(TourProcessingDraftStatus.DRAFT).build();
    }

    static TourProcessingDraftTour tour(TourProcessingDraft draft, int row, int priority, int passengers, String date) {
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .draft(draft)
                .rowNumber(row)
                .number(String.valueOf(1000 + row))
                .passengers(passengers)
                .executingUnit("Escuela de Química")
                .responsible("Responsable")
                .destination("Cartago")
                .durationDays(1)
                .priority(priority)
                .departureTime(LocalTime.of(8, 0))
                .returnTime(LocalTime.of(15, 0))
                .departureDate(LocalDate.parse(date))
                .returnDate(LocalDate.parse(date))
                .duplicateRank(1)
                .duplicateGroupSize(1)
                .build();
        tour.addAssignment(TourProcessingDraftAssignment.builder()
                .id(UUID.randomUUID())
                .passengers(passengers)
                .resourceType(ResourceType.INTERNAL)
                .build());
        return tour;
    }

    static TourProcessingDraftTour withDriver(TourProcessingDraftTour tour, String name, boolean automatic) {
        TourProcessingDraftAssignment part = tour.primaryAssignment();
        part.setDriver(name);
        part.setDriverId(UUID.randomUUID());
        part.setDriverAutoAssigned(automatic);
        return tour;
    }

    static TourProcessingDraftTour withVehicle(TourProcessingDraftTour tour, String plate, boolean automatic) {
        TourProcessingDraftAssignment part = tour.primaryAssignment();
        part.setVehicle(plate);
        part.setVehicleId(UUID.randomUUID());
        part.setVehicleAutoAssigned(automatic);
        return tour;
    }

    static TourProcessingDraftTour peripheral(TourProcessingDraft draft, int row, String date) {
        TourProcessingDraftTour tour = tour(draft, row, 6, 30, date);
        tour.setExecutingUnit("Programa De Servicios Generales");
        tour.setDestination("Ulloa (barreal)");
        tour.setDepartureTime(LocalTime.of(6, 30));
        tour.setReturnTime(LocalTime.of(17, 30));
        return tour;
    }
}
