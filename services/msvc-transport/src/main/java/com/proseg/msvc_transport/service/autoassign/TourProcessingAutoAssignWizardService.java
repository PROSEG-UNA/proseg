package com.proseg.msvc_transport.service.autoassign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingAutoAssignWizardDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDriverAutoAssignResponseDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingVehicleAutoAssignResponseDto;
import com.proseg.msvc_transport.entity.ResourceType;
import com.proseg.msvc_transport.entity.TourProcessingAutoAssignSession;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.TourProcessingAutoAssignSessionRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftDayRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftRepository;
import com.proseg.msvc_transport.repository.TourProcessingDraftTourRepository;
import com.proseg.msvc_transport.service.DraftHoursService;
import com.proseg.msvc_transport.service.TourProcessingDraftService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourProcessingAutoAssignWizardService {

    private static final String DRAFT_RESOURCE = "Borrador de depuración";
    private static final String SESSION_RESOURCE = "Asistente de autoasignación";

    private static final String SNAPSHOT_SQL = """
            select jsonb_build_object(
                'draft', (select to_jsonb(d) from tour_processing_draft_table d where d.id = ?),
                'tours', coalesce((select jsonb_agg(to_jsonb(t)) from tour_processing_draft_tour_table t where t.draft_id = ?), '[]'::jsonb),
                'import_rows', coalesce((select jsonb_agg(to_jsonb(r)) from tour_processing_draft_import_row_table r where r.draft_id = ?), '[]'::jsonb),
                'assignments', coalesce((select jsonb_agg(to_jsonb(a)) from tour_processing_draft_assignment_table a
                    join tour_processing_draft_tour_table t on t.id = a.tour_id where t.draft_id = ?), '[]'::jsonb),
                'days', coalesce((select jsonb_agg(to_jsonb(x)) from tour_processing_draft_day_table x
                    join tour_processing_draft_assignment_table a on a.id = x.assignment_id
                    join tour_processing_draft_tour_table t on t.id = a.tour_id where t.draft_id = ?), '[]'::jsonb)
            )::text
            """;

    private static final String DELETE_DAYS_SQL = """
            delete from tour_processing_draft_day_table where assignment_id in (
                select a.id from tour_processing_draft_assignment_table a
                join tour_processing_draft_tour_table t on t.id = a.tour_id where t.draft_id = ?)
            """;

    private static final String DELETE_ASSIGNMENTS_SQL = """
            delete from tour_processing_draft_assignment_table where tour_id in (
                select id from tour_processing_draft_tour_table where draft_id = ?)
            """;

    private static final String DELETE_TOURS_SQL = "delete from tour_processing_draft_tour_table where draft_id = ?";

    private static final String DELETE_IMPORT_ROWS_SQL = "delete from tour_processing_draft_import_row_table where draft_id = ?";

    private static final String INSERT_IMPORT_ROWS_SQL = """
            insert into tour_processing_draft_import_row_table
            select * from jsonb_populate_recordset(null::tour_processing_draft_import_row_table, cast(? as jsonb) -> 'import_rows')
            """;

    private static final String INSERT_TOURS_SQL = """
            insert into tour_processing_draft_tour_table
            select * from jsonb_populate_recordset(null::tour_processing_draft_tour_table, cast(? as jsonb) -> 'tours')
            """;

    private static final String INSERT_ASSIGNMENTS_SQL = """
            insert into tour_processing_draft_assignment_table
            select * from jsonb_populate_recordset(null::tour_processing_draft_assignment_table, cast(? as jsonb) -> 'assignments')
            """;

    private static final String INSERT_DAYS_SQL = """
            insert into tour_processing_draft_day_table
            select * from jsonb_populate_recordset(null::tour_processing_draft_day_table, cast(? as jsonb) -> 'days')
            """;

    private static final String RESTORE_DRAFT_SQL = """
            update tour_processing_draft_table set
                total_rows = (s.d ->> 'total_rows')::int,
                duplicate_rows = (s.d ->> 'duplicate_rows')::int,
                conflicting_rows = (s.d ->> 'conflicting_rows')::int,
                blocks_detected = (s.d ->> 'blocks_detected')::int,
                min_departure_date = (s.d ->> 'min_departure_date')::date,
                max_return_date = (s.d ->> 'max_return_date')::date
            from (select cast(? as jsonb) -> 'draft' as d) s
            where id = ?
            """;

    private final TourProcessingDraftRepository draftRepository;
    private final TourProcessingDraftTourRepository tourRepository;
    private final TourProcessingDraftDayRepository dayRepository;
    private final TourProcessingAutoAssignSessionRepository sessionRepository;
    private final TourProcessingAutoAssignSteps steps;
    private final TourProcessingDraftService draftService;
    private final DraftHoursService draftHoursService;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TourProcessingAutoAssignWizardDto state(UUID draftId) {
        return sessionRepository.findByDraftId(draftId).map(this::stateOf).orElseGet(TourProcessingAutoAssignWizardDto::inactive);
    }

    @Transactional
    public TourProcessingAutoAssignWizardDto start(UUID draftId) {
        editableDraft(draftId);
        if (sessionRepository.findByDraftId(draftId).isPresent()) {
            return state(draftId);
        }
        Set<UUID> scope = steps.scopeOf(draftId);
        if (scope.isEmpty()) {
            throw TransportException.unprocessable(
                    "TOUR_PROCESSING_AUTO_ASSIGN_NOTHING_PENDING",
                    "No hay giras aprobadas con chofer o vehículo pendiente"
            );
        }
        entityManager.flush();
        TourProcessingAutoAssignSession session = sessionRepository.save(TourProcessingAutoAssignSession.builder()
                .draftId(draftId)
                .scopeTourIds(TourProcessingAutoAssignSteps.formatScope(scope))
                .snapshot(snapshotOf(draftId))
                .build());
        calculate(draftId, session);
        return stateOf(session);
    }

    @Transactional
    public TourProcessingAutoAssignWizardDto recalculate(UUID draftId) {
        editableDraft(draftId);
        TourProcessingAutoAssignSession session = requireSession(draftId);
        calculate(draftId, session);
        return stateOf(session);
    }

    @Transactional
    public TourProcessingAutoAssignWizardDto setStep(UUID draftId, String currentStep) {
        editableDraft(draftId);
        TourProcessingAutoAssignSession session = requireSession(draftId);
        session.setCurrentStep(validStep(currentStep));
        sessionRepository.save(session);
        return stateOf(session);
    }

    @Transactional
    public TourProcessingAutoAssignWizardDto save(UUID draftId, String currentStep) {
        editableDraft(draftId);
        TourProcessingAutoAssignSession session = requireSession(draftId);
        session.setCurrentStep(validStep(currentStep));
        entityManager.flush();
        session.setSnapshot(snapshotOf(draftId));
        sessionRepository.save(session);
        return stateOf(session);
    }

    private static String validStep(String currentStep) {
        if (currentStep != null && !AutoAssignStepCatalog.isStepCode(currentStep)) {
            throw TransportException.badRequest("TOUR_PROCESSING_AUTO_ASSIGN_STEP_UNKNOWN", "No existe el paso " + currentStep);
        }
        return currentStep;
    }

    @Transactional
    public void endSession(UUID draftId) {
        sessionRepository.findByDraftId(draftId).ifPresent(sessionRepository::delete);
    }

    @Transactional
    public void finish(UUID draftId) {
        editableDraft(draftId);
        TourProcessingAutoAssignSession session = requireSession(draftId);
        tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .flatMap(tour -> tour.getAssignments().stream())
                .forEach(assignment -> {
                    assignment.setDriverAutoAssigned(false);
                    assignment.setVehicleAutoAssigned(false);
                });
        sessionRepository.delete(session);
    }

    @Transactional
    public void discard(UUID draftId) {
        editableDraft(draftId);
        TourProcessingAutoAssignSession session = requireSession(draftId);
        entityManager.flush();
        restore(draftId, session.getSnapshot());
        sessionRepository.delete(session);
        entityManager.flush();
        entityManager.clear();
        TourProcessingDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString()));
        List<TourProcessingDraftAssignment> assignments = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .flatMap(tour -> tour.getAssignments().stream())
                .toList();
        draftHoursService.syncAll(draft, assignments);
    }

    private void calculate(UUID draftId, TourProcessingAutoAssignSession session) {
        draftService.clearAutoAssignments(draftId);
        TourProcessingDriverAutoAssignResponseDto drivers = draftService.autoAssignDrivers(draftId);
        TourProcessingVehicleAutoAssignResponseDto vehicles = draftService.autoAssignVehiclesForAssignedDrivers(draftId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("drivers", drivers);
        result.put("vehicles", vehicles);
        session.setLastResult(toJson(result));
        session.setCalculatedFingerprint(fingerprintOf(draftId));
        List<TourProcessingAutoAssignSteps.StepGroup> groups = steps.groups(draftId);
        if (session.getCurrentStep() == null || groups.stream().noneMatch(group -> group.code().equals(session.getCurrentStep()))) {
            session.setCurrentStep(groups.stream().map(TourProcessingAutoAssignSteps.StepGroup::code).findFirst().orElse(null));
        }
        sessionRepository.save(session);
    }

    private TourProcessingAutoAssignWizardDto stateOf(TourProcessingAutoAssignSession session) {
        UUID draftId = session.getDraftId();
        List<TourProcessingAutoAssignSteps.StepGroup> groups = steps.groups(draftId);
        List<TourProcessingAutoAssignWizardDto.Step> stepDtos = groups.stream().map(this::stepOf).toList();
        String current = stepDtos.stream().map(TourProcessingAutoAssignWizardDto.Step::code)
                .filter(code -> code.equals(session.getCurrentStep())).findFirst()
                .orElseGet(() -> stepDtos.isEmpty() ? null : stepDtos.get(0).code());
        boolean dirty = !fingerprintOf(draftId).equals(session.getCalculatedFingerprint());
        return new TourProcessingAutoAssignWizardDto(true, current, dirty, stepDtos, readJson(session.getLastResult()));
    }

    private TourProcessingAutoAssignWizardDto.Step stepOf(TourProcessingAutoAssignSteps.StepGroup group) {
        int complete = 0;
        int withoutDriver = 0;
        int withoutVehicle = 0;
        Map<String, List<String>> numbersByDriver = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (TourProcessingDraftTour tour : group.tours()) {
            List<TourProcessingDraftAssignment> internal = tour.getAssignments().stream()
                    .filter(part -> part.getResourceType() == ResourceType.INTERNAL).toList();
            boolean missingDriver = internal.stream().anyMatch(part -> part.getDriverId() == null);
            boolean missingVehicle = internal.stream().anyMatch(part -> part.getVehicleId() == null);
            if (missingDriver) {
                withoutDriver++;
            } else if (missingVehicle) {
                withoutVehicle++;
            } else {
                complete++;
            }
            internal.stream().map(TourProcessingDraftAssignment::getDriver).filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .forEach(name -> numbersByDriver.computeIfAbsent(name, key -> new ArrayList<>()).add(tour.getNumber()));
        }
        List<TourProcessingAutoAssignWizardDto.DriverLoad> drivers = numbersByDriver.entrySet().stream()
                .map(entry -> new TourProcessingAutoAssignWizardDto.DriverLoad(entry.getKey(), entry.getValue().size(), entry.getValue()))
                .sorted(Comparator.comparingInt(TourProcessingAutoAssignWizardDto.DriverLoad::tours).reversed()
                        .thenComparing(TourProcessingAutoAssignWizardDto.DriverLoad::driver, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new TourProcessingAutoAssignWizardDto.Step(
                group.code(),
                AutoAssignStepCatalog.labelOf(group.code()),
                AutoAssignStepCatalog.FILTER_PREFIX + group.code(),
                group.tours().size(),
                complete,
                withoutDriver,
                withoutVehicle,
                drivers
        );
    }

    String fingerprintOf(UUID draftId) {
        StringBuilder canonical = new StringBuilder();
        List<TourProcessingDraftTour> tours = tourRepository.findAllByDraftIdOrderByRowNumberAsc(draftId).stream()
                .sorted(Comparator.comparing(TourProcessingDraftTour::getId)).toList();
        for (TourProcessingDraftTour tour : tours) {
            canonical.append(tour.getId()).append('|').append(tour.getNumber()).append('|').append(tour.getPriority()).append('|')
                    .append(tour.getPassengers()).append('|').append(tour.getApprovalStatus()).append('|').append(tour.getDestination()).append('|')
                    .append(tour.getDepartureDate()).append(tour.getDepartureTime()).append(tour.getReturnDate()).append(tour.getReturnTime()).append('|')
                    .append(tour.getDuplicateRank()).append('|').append(tour.isRepeatsTrips()).append('\n');
            for (TourProcessingDraftAssignment part : tour.getAssignments()) {
                canonical.append(" ").append(part.getPart()).append('|').append(part.getPassengers()).append('|').append(part.getResourceType()).append('|')
                        .append(part.isDriverAutoAssigned() || part.getDriverId() == null ? "-" : part.getDriverId()).append('|')
                        .append(part.isVehicleAutoAssigned() || part.getVehicleId() == null ? "-" : part.getVehicleId()).append('|')
                        .append(part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getId()).append('\n');
            }
        }
        List<TourProcessingDraftDay> manualDays = dayRepository.findAllByDraftId(draftId).stream()
                .filter(TourProcessingDraftDay::isManuallyEdited)
                .sorted(Comparator.comparing((TourProcessingDraftDay day) -> day.getAssignment().getId()).thenComparing(TourProcessingDraftDay::getDate))
                .toList();
        for (TourProcessingDraftDay day : manualDays) {
            canonical.append(day.getAssignment().getId()).append('|').append(day.getDate()).append('|').append(day.getStartTime()).append('|')
                    .append(day.getEndTime()).append('|').append(day.getOrdinaryHours()).append('|').append(day.getSurplusHours()).append('|')
                    .append(day.getOvertimeHours()).append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String snapshotOf(UUID draftId) {
        return jdbcTemplate.queryForObject(SNAPSHOT_SQL, String.class, draftId, draftId, draftId, draftId, draftId);
    }

    private void restore(UUID draftId, String snapshot) {
        if (snapshot == null) {
            return;
        }
        jdbcTemplate.update(DELETE_DAYS_SQL, draftId);
        jdbcTemplate.update(DELETE_ASSIGNMENTS_SQL, draftId);
        jdbcTemplate.update(DELETE_TOURS_SQL, draftId);
        jdbcTemplate.update(DELETE_IMPORT_ROWS_SQL, draftId);
        jdbcTemplate.update(INSERT_TOURS_SQL, snapshot);
        jdbcTemplate.update(INSERT_IMPORT_ROWS_SQL, snapshot);
        jdbcTemplate.update(INSERT_ASSIGNMENTS_SQL, snapshot);
        jdbcTemplate.update(INSERT_DAYS_SQL, snapshot);
        jdbcTemplate.update(RESTORE_DRAFT_SQL, snapshot, draftId);
    }

    private TourProcessingDraft editableDraft(UUID draftId) {
        TourProcessingDraft draft = draftRepository.findByIdForUpdate(draftId)
                .orElseThrow(() -> TransportException.notFound(DRAFT_RESOURCE, draftId.toString()));
        if (draft.getStatus() != TourProcessingDraftStatus.DRAFT) {
            throw TransportException.conflict(
                    "TOUR_PROCESSING_DRAFT_NOT_EDITABLE",
                    "Solo se puede autoasignar en un borrador abierto"
            );
        }
        return draft;
    }

    private TourProcessingAutoAssignSession requireSession(UUID draftId) {
        return sessionRepository.findByDraftId(draftId)
                .orElseThrow(() -> TransportException.notFound(SESSION_RESOURCE, draftId.toString()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private JsonNode readJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
