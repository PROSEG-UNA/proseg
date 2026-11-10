package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.repository.TourProcessingDraftAssignmentRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftTourRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DraftCatalogService {
    private final TourProcessingDraftAssignmentRepository assignmentRepository;
    private final TourProcessingDraftTourRepository tourRepository;
    private final DraftHoursService hoursService;

    @Transactional
    public void syncDriver(Driver driver) {
        assignmentRepository.linkDriver(driver.getId(), driver.getName(), DriverHoursRules.driverKey(driver.getName()));
        Map<UUID, TourProcessingDraft> drafts = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment assignment : assignmentRepository.findAllByDriverId(driver.getId())) {
            TourProcessingDraft draft = assignment.getTour().getDraft();
            if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) continue;
            assignment.setDriver(driver.getName());
            drafts.put(draft.getId(), draft);
        }
        drafts.values().forEach(draft -> hoursService.syncDriver(draft,
                tourRepository.findAllByDraftIdOrderByRowNumberAsc(draft.getId()).stream()
                        .flatMap(tour -> tour.getAssignments().stream()).toList(), driver.getId()));
    }

    @Transactional
    public void removeDriver(UUID id) {
        Map<UUID, TourProcessingDraft> drafts = new LinkedHashMap<>();
        for (TourProcessingDraftAssignment assignment : assignmentRepository.findAllByDriverId(id)) {
            TourProcessingDraft draft = assignment.getTour().getDraft();
            if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) continue;
            assignment.setDriverId(null);
            drafts.put(draft.getId(), draft);
        }
        drafts.values().forEach(draft -> hoursService.syncDriver(draft,
                tourRepository.findAllByDraftIdOrderByRowNumberAsc(draft.getId()).stream()
                        .flatMap(tour -> tour.getAssignments().stream()).toList(), id));
    }

    @Transactional
    public void syncVehicle(Vehicle vehicle) {
        assignmentRepository.linkVehicle(vehicle.getId(), vehicle.getPlate(), DriverHoursRules.plateKey(vehicle.getPlate()));
        assignmentRepository.renameVehicle(vehicle.getId(), vehicle.getPlate(), TourProcessingDraftStatus.DRAFT);
    }

    @Transactional
    public int clearVehicleFromOpenDrafts(UUID id) {
        Map<UUID, TourProcessingDraft> drafts = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> driverIdsByDraft = new LinkedHashMap<>();
        int cleared = 0;
        for (TourProcessingDraftAssignment assignment : assignmentRepository.findByVehicleIdAndDraftStatus(id, TourProcessingDraftStatus.DRAFT)) {
            TourProcessingDraft draft = assignment.getTour().getDraft();
            assignment.setVehicle(null);
            assignment.setVehicleId(null);
            drafts.put(draft.getId(), draft);
            Set<UUID> driverIds = driverIdsByDraft.computeIfAbsent(draft.getId(), draftId -> new LinkedHashSet<>());
            if (assignment.getDriverId() != null) driverIds.add(assignment.getDriverId());
            cleared++;
        }
        drafts.forEach((draftId, draft) -> {
            List<TourProcessingDraftAssignment> assignments = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                    .flatMap(tour -> tour.getAssignments().stream()).toList();
            driverIdsByDraft.get(draftId).forEach(driverId -> hoursService.syncDriver(draft, assignments, driverId));
        });
        return cleared;
    }

    @Transactional
    public void removeVehicle(UUID id) {
        assignmentRepository.unlinkVehicle(id, TourProcessingDraftStatus.DRAFT);
    }
}
