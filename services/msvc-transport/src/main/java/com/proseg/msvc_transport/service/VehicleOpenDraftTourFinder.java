package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.response.VehicleOpenDraftTourResponseDto;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleOpenDraftTourFinder {

    private final TourProcessingDraftAssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<VehicleOpenDraftTourResponseDto> find(UUID vehicleId) {
        Map<UUID, VehicleOpenDraftTourResponseDto> tours = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment assignment : assignmentRepository.findByVehicleIdAndDraftStatus(vehicleId, TourProcessingDraftStatus.DRAFT)) {
            if (!assignment.isActive()) {
                continue;
            }
            TourProcessingDraftTour tour = assignment.getTour();
            tours.putIfAbsent(tour.getId(), toResponse(tour));
        }
        return tours.values().stream()
                .sorted(Comparator.comparing(VehicleOpenDraftTourResponseDto::getDepartureDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VehicleOpenDraftTourResponseDto::getNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private VehicleOpenDraftTourResponseDto toResponse(TourProcessingDraftTour tour) {
        TourProcessingDraft draft = tour.getDraft();
        return VehicleOpenDraftTourResponseDto.builder()
                .draftId(draft.getId())
                .year(draft.getYear())
                .month(draft.getMonth())
                .fileName(draft.getFileName())
                .number(tour.getNumber())
                .departureDate(tour.getDepartureDate())
                .build();
    }
}
