package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.VehicleOpenDraftTourResponseDto;
import com.proseg.msvc_transport.entity.TourApprovalStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleOpenDraftTourFinderTest {

    @Test
    void reportsEachOpenDraftTourOnce() {
        UUID vehicleId = UUID.randomUUID();
        UUID tourId = UUID.randomUUID();
        TourProcessingDraft draft = TourProcessingDraft.builder()
                .id(UUID.randomUUID())
                .year(2026)
                .month(5)
                .fileName("mayo.xlsx")
                .status(TourProcessingDraftStatus.DRAFT)
                .build();
        TourProcessingDraftTour tour = TourProcessingDraftTour.builder()
                .id(tourId)
                .draft(draft)
                .number("05100")
                .departureDate(LocalDate.of(2026, 5, 4))
                .build();
        TourProcessingDraftAssignment first = TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).tour(tour).vehicleId(vehicleId).build();
        TourProcessingDraftAssignment second = TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).tour(tour).vehicleId(vehicleId).build();
        TourProcessingDraftAssignmentRepository repository = mock(TourProcessingDraftAssignmentRepository.class);
        when(repository.findByVehicleIdAndDraftStatus(vehicleId, TourProcessingDraftStatus.DRAFT)).thenReturn(List.of(second, first));

        List<VehicleOpenDraftTourResponseDto> tours = new VehicleOpenDraftTourFinder(repository).find(vehicleId);

        assertThat(tours).hasSize(1);
        assertThat(tours.get(0).getNumber()).isEqualTo("05100");
        assertThat(tours.get(0).getYear()).isEqualTo(2026);
        assertThat(tours.get(0).getMonth()).isEqualTo(5);
        assertThat(tours.get(0).getDepartureDate()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(tours.get(0).getFileName()).isEqualTo("mayo.xlsx");
    }

    @Test
    void skipsDuplicateAndNotApprovedTours() {
        UUID vehicleId = UUID.randomUUID();
        TourProcessingDraft draft = TourProcessingDraft.builder()
                .id(UUID.randomUUID())
                .year(2026)
                .month(5)
                .fileName("mayo.xlsx")
                .status(TourProcessingDraftStatus.DRAFT)
                .build();
        TourProcessingDraftTour active = tour(draft, "05100", 1, TourApprovalStatus.APPROVED);
        TourProcessingDraftTour duplicate = tour(draft, "05100", 2, TourApprovalStatus.APPROVED);
        TourProcessingDraftTour rejected = tour(draft, "05200", 1, TourApprovalStatus.NOT_APPROVED);
        TourProcessingDraftAssignmentRepository repository = mock(TourProcessingDraftAssignmentRepository.class);
        when(repository.findByVehicleIdAndDraftStatus(vehicleId, TourProcessingDraftStatus.DRAFT)).thenReturn(List.of(
                assignment(duplicate, vehicleId),
                assignment(rejected, vehicleId),
                assignment(active, vehicleId)
        ));

        List<VehicleOpenDraftTourResponseDto> tours = new VehicleOpenDraftTourFinder(repository).find(vehicleId);

        assertThat(tours).extracting(VehicleOpenDraftTourResponseDto::getNumber).containsExactly("05100");
    }

    private static TourProcessingDraftTour tour(TourProcessingDraft draft, String number, int duplicateRank, TourApprovalStatus status) {
        return TourProcessingDraftTour.builder()
                .id(UUID.randomUUID())
                .draft(draft)
                .number(number)
                .duplicateRank(duplicateRank)
                .approvalStatus(status)
                .departureDate(LocalDate.of(2026, 5, 4))
                .build();
    }

    private static TourProcessingDraftAssignment assignment(TourProcessingDraftTour tour, UUID vehicleId) {
        return TourProcessingDraftAssignment.builder().id(UUID.randomUUID()).tour(tour).vehicleId(vehicleId).build();
    }
}
