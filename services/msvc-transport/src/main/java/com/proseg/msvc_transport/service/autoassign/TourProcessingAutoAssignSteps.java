package com.proseg.msvc_transport.service.autoassign;

import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingAutoAssignSession;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.repository.TourProcessingAutoAssignSessionRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftTourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TourProcessingAutoAssignSteps {

    private final TourProcessingDraftTourRepository tourRepository;
    private final TourProcessingDraftDayRepository dayRepository;
    private final TourProcessingAutoAssignSessionRepository sessionRepository;

    public record StepGroup(String code, List<TourProcessingDraftTour> tours) {
    }

    @Transactional(readOnly = true)
    public Set<UUID> tourIds(UUID draftId, String code) {
        return groups(draftId).stream()
                .filter(group -> group.code().equals(code))
                .flatMap(group -> group.tours().stream())
                .map(TourProcessingDraftTour::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public List<StepGroup> groups(UUID draftId) {
        return sessionRepository.findByDraftId(draftId)
                .map(session -> groupsOf(draftId, parseScope(session)))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Set<UUID> scopeOf(UUID draftId) {
        return tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .filter(TourProcessingDraftTour::isPrimary)
                .filter(TourProcessingDraftTour::isApproved)
                .filter(TourProcessingAutoAssignSteps::lacksAssignment)
                .map(TourProcessingDraftTour::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    List<StepGroup> groupsOf(UUID draftId, Set<UUID> scope) {
        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .filter(tour -> scope.contains(tour.getId()))
                .filter(TourProcessingDraftTour::isPrimary)
                .filter(TourProcessingDraftTour::isApproved)
                .toList();
        Map<UUID, List<TourProcessingDraftDay>> daysByAssignmentId = new HashMap<>();
        dayRepository.findAllByDraftId(draftId).forEach(day ->
                daysByAssignmentId.computeIfAbsent(day.getAssignment().getId(), id -> new ArrayList<>()).add(day));
        Map<String, List<TourProcessingDraftTour>> byCode = new TreeMap<>(AutoAssignStepCatalog.STEP_ORDER);
        for (TourProcessingDraftTour tour : tours) {
            byCode.computeIfAbsent(AutoAssignStepCatalog.stepCodeOf(tour, daysByAssignmentId), code -> new ArrayList<>()).add(tour);
        }
        List<StepGroup> groups = new ArrayList<>();
        byCode.forEach((code, list) -> groups.add(new StepGroup(code, list)));
        return groups;
    }

    public static Set<UUID> parseScope(TourProcessingAutoAssignSession session) {
        String text = session.getScopeTourIds();
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (String id : text.split(",")) {
            ids.add(UUID.fromString(id.trim()));
        }
        return ids;
    }

    public static String formatScope(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    static boolean lacksAssignment(TourProcessingDraftTour tour) {
        return tour.getAssignments().stream().anyMatch(part -> part.getResourceType() == ResourceType.INTERNAL
                && (part.getDriverId() == null || part.getVehicleId() == null));
    }
}
