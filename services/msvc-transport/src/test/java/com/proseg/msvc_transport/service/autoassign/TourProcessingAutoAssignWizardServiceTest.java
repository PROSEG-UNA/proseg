package com.proseg.msvc_transport.service.autoassign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingAutoAssignWizardDto;
import com.proseg.msvc_transport.entity.TourProcessingAutoAssignSession;
import com.proseg.msvc_transport.entity.TourProcessingDraft;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.draft;
import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.peripheral;
import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.tour;
import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.withDriver;
import static com.proseg.msvc_transport.service.autoassign.AutoAssignTestRows.withVehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourProcessingAutoAssignWizardServiceTest {

    private static final String SNAPSHOT = "{\"tours\":[],\"assignments\":[],\"days\":[],\"import_rows\":[],\"draft\":{}}";

    private TourProcessingDraft draft;
    private List<TourProcessingDraftTour> tours;
    private AtomicReference<TourProcessingAutoAssignSession> stored;
    private TourProcessingDraftRepository draftRepository;
    private TourProcessingDraftTourRepository tourRepository;
    private TourProcessingDraftDayRepository dayRepository;
    private TourProcessingAutoAssignSessionRepository sessionRepository;
    private TourProcessingDraftService draftService;
    private DraftHoursService draftHoursService;
    private JdbcTemplate jdbcTemplate;
    private TourProcessingAutoAssignWizardService service;

    @BeforeEach
    void setUp() {
        draft = draft();
        tours = new ArrayList<>();
        stored = new AtomicReference<>();
        draftRepository = mock(TourProcessingDraftRepository.class);
        tourRepository = mock(TourProcessingDraftTourRepository.class);
        dayRepository = mock(TourProcessingDraftDayRepository.class);
        sessionRepository = mock(TourProcessingAutoAssignSessionRepository.class);
        draftService = mock(TourProcessingDraftService.class);
        draftHoursService = mock(DraftHoursService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        when(draftRepository.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(tourRepository.findAllByDraftIdOrderByRowNumberAsc(draft.getId())).thenAnswer(invocation -> List.copyOf(tours));
        when(dayRepository.findAllByDraftId(draft.getId())).thenReturn(List.of());
        when(sessionRepository.save(any(TourProcessingAutoAssignSession.class))).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(sessionRepository.findByDraftId(draft.getId())).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        org.mockito.Mockito.doAnswer(invocation -> {
            stored.set(null);
            return null;
        }).when(sessionRepository).delete(any(TourProcessingAutoAssignSession.class));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(), any(), any(), any(), any())).thenReturn(SNAPSHOT);
        TourProcessingAutoAssignSteps steps = new TourProcessingAutoAssignSteps(tourRepository, dayRepository, sessionRepository);
        service = new TourProcessingAutoAssignWizardService(draftRepository, tourRepository, dayRepository, sessionRepository, steps,
                draftService, draftHoursService, jdbcTemplate, mock(EntityManager.class), new ObjectMapper());
    }

    private TourProcessingDraftTour add(TourProcessingDraftTour tour) {
        tours.add(tour);
        return tour;
    }

    @Test
    void refusesToStartWhenEveryTourAlreadyHasDriverAndVehicle() {
        add(withVehicle(withDriver(tour(draft, 1, 1, 12, "2026-05-06"), "Ana", false), "301-295", false));

        assertThatThrownBy(() -> service.start(draft.getId()))
                .isInstanceOf(TransportException.class)
                .hasMessage("No hay giras aprobadas con chofer o vehículo pendiente");
        verify(draftService, never()).autoAssignDrivers(any());
    }

    @Test
    void refusesToStartOnAClosedDraft() {
        draft.setStatus(TourProcessingDraftStatus.CLOSED);
        add(tour(draft, 1, 1, 12, "2026-05-06"));

        assertThatThrownBy(() -> service.start(draft.getId())).isInstanceOf(TransportException.class);
    }

    @Test
    void startsBySnapshottingClearingAutomaticAssignmentsAndRunningDriversThenVehicles() {
        TourProcessingDraftTour mandatory = add(tour(draft, 1, 1, 3, "2026-05-06"));
        add(tour(draft, 2, 2, 12, "2026-05-07"));
        add(withVehicle(withDriver(tour(draft, 3, 2, 12, "2026-05-08"), "Ana", false), "301-295", false));

        TourProcessingAutoAssignWizardDto state = service.start(draft.getId());

        InOrder order = inOrder(jdbcTemplate, draftService);
        order.verify(jdbcTemplate).queryForObject(anyString(), eq(String.class), any(), any(), any(), any(), any());
        order.verify(draftService).clearAutoAssignments(draft.getId());
        order.verify(draftService).autoAssignDrivers(draft.getId());
        order.verify(draftService).autoAssignVehiclesForAssignedDrivers(draft.getId());
        assertThat(state.active()).isTrue();
        assertThat(state.dirty()).isFalse();
        assertThat(state.steps()).extracting(TourProcessingAutoAssignWizardDto.Step::code).containsExactly("MANDATORY", "PRIORITY_2");
        assertThat(state.currentStep()).isEqualTo("MANDATORY");
        assertThat(stored.get().getScopeTourIds()).contains(mandatory.getId().toString());
        assertThat(stored.get().getSnapshot()).isEqualTo(SNAPSHOT);
    }

    @Test
    void listsOnlyToursThatLackedDriverOrVehicleAtTheStart() {
        TourProcessingDraftTour pending = add(tour(draft, 1, 2, 12, "2026-05-06"));
        TourProcessingDraftTour driverOnly = add(withDriver(tour(draft, 2, 2, 12, "2026-05-07"), "Ana", false));
        add(withVehicle(withDriver(tour(draft, 3, 2, 12, "2026-05-08"), "Beto", false), "301-295", false));

        TourProcessingAutoAssignWizardDto state = service.start(draft.getId());

        assertThat(state.steps()).hasSize(1);
        TourProcessingAutoAssignWizardDto.Step step = state.steps().get(0);
        assertThat(step.tours()).isEqualTo(2);
        assertThat(step.withoutDriver()).isEqualTo(1);
        assertThat(step.withoutVehicle()).isEqualTo(1);
        assertThat(step.complete()).isZero();
        assertThat(step.drivers()).extracting(TourProcessingAutoAssignWizardDto.DriverLoad::driver).containsExactly("Ana");
        assertThat(step.filterRule()).isEqualTo("WIZARD_STEP:PRIORITY_2");
        assertThat(pending.getId()).isNotNull();
        assertThat(driverOnly.getId()).isNotNull();
    }

    @Test
    void summarizesTheDriversOfEachStepWithTheirTourNumbers() {
        withDriver(add(tour(draft, 1, 1, 3, "2026-05-06")), "Ana", true);
        withDriver(add(tour(draft, 2, 1, 2, "2026-05-07")), "Ana", true);
        withDriver(add(tour(draft, 3, 1, 4, "2026-05-08")), "Beto", true);

        TourProcessingAutoAssignWizardDto.Step step = service.start(draft.getId()).steps().get(0);

        assertThat(step.drivers()).extracting(TourProcessingAutoAssignWizardDto.DriverLoad::driver, TourProcessingAutoAssignWizardDto.DriverLoad::tours)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Ana", 2), org.assertj.core.groups.Tuple.tuple("Beto", 1));
        assertThat(step.drivers().get(0).tourNumbers()).containsExactly("1001", "1002");
    }

    @Test
    void startingAgainResumesTheSavedSessionWithoutRecalculating() {
        add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());

        TourProcessingAutoAssignWizardDto resumed = service.start(draft.getId());

        assertThat(resumed.active()).isTrue();
        verify(draftService, times(1)).autoAssignDrivers(draft.getId());
    }

    @Test
    void reportsNoSessionWhenNothingWasStarted() {
        assertThat(service.state(draft.getId()).active()).isFalse();
    }

    @Test
    void becomesDirtyWhenTheUserChangesADriverByHandButNotWhenOnlyAutomaticDataChanges() {
        TourProcessingDraftTour pending = add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());
        withDriver(pending, "Ana", true);
        assertThat(service.state(draft.getId()).dirty()).isFalse();

        pending.primaryAssignment().setDriverAutoAssigned(false);

        assertThat(service.state(draft.getId()).dirty()).isTrue();
    }

    @Test
    void recalculatingClearsDirtyAndRunsTheAlgorithmAgain() {
        TourProcessingDraftTour pending = add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());
        withDriver(pending, "Ana", false);
        assertThat(service.state(draft.getId()).dirty()).isTrue();

        TourProcessingAutoAssignWizardDto state = service.recalculate(draft.getId());

        assertThat(state.dirty()).isFalse();
        verify(draftService, times(2)).clearAutoAssignments(draft.getId());
        verify(draftService, times(2)).autoAssignVehiclesForAssignedDrivers(draft.getId());
    }

    @Test
    void movingToAnotherStepStoresItWithoutTouchingTheBaselineSnapshot() {
        add(tour(draft, 1, 1, 3, "2026-05-06"));
        add(peripheral(draft, 2, "2026-05-07"));
        service.start(draft.getId());

        TourProcessingAutoAssignWizardDto state = service.setStep(draft.getId(), "PERIPHERAL");

        assertThat(state.currentStep()).isEqualTo("PERIPHERAL");
        assertThat(stored.get().getCurrentStep()).isEqualTo("PERIPHERAL");
        assertThat(stored.get().getSnapshot()).isEqualTo(SNAPSHOT);
    }

    @Test
    void savingAndExitingStoresTheStepAndRetakesTheBaselineSnapshot() {
        add(tour(draft, 1, 1, 3, "2026-05-06"));
        add(peripheral(draft, 2, "2026-05-07"));
        service.start(draft.getId());
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(), any(), any(), any(), any())).thenReturn("{\"saved\":true}");

        TourProcessingAutoAssignWizardDto state = service.save(draft.getId(), "PERIPHERAL");

        assertThat(state.currentStep()).isEqualTo("PERIPHERAL");
        assertThat(stored.get().getSnapshot()).isEqualTo("{\"saved\":true}");
    }

    @Test
    void rejectsAnUnknownStepWhenMovingOrSaving() {
        add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());

        assertThatThrownBy(() -> service.setStep(draft.getId(), "NOPE"))
                .isInstanceOf(TransportException.class)
                .hasMessage("No existe el paso NOPE");
        assertThatThrownBy(() -> service.save(draft.getId(), "NOPE")).isInstanceOf(TransportException.class);
    }

    @Test
    void finishingKeepsTheAssignmentsAsFixedAndDeletesTheSession() {
        TourProcessingDraftTour pending = add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());
        withVehicle(withDriver(pending, "Ana", true), "301-295", true);

        service.finish(draft.getId());

        assertThat(pending.primaryAssignment().isDriverAutoAssigned()).isFalse();
        assertThat(pending.primaryAssignment().isVehicleAutoAssigned()).isFalse();
        assertThat(pending.primaryAssignment().getDriver()).isEqualTo("Ana");
        assertThat(stored.get()).isNull();
    }

    @Test
    void discardingRestoresTheSnapshotRecalculatesHoursAndDeletesTheSession() {
        add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());

        service.discard(draft.getId());

        verify(jdbcTemplate, times(4)).update(anyString(), eq(SNAPSHOT));
        verify(jdbcTemplate).update(anyString(), eq(SNAPSHOT), eq(draft.getId()));
        verify(draftHoursService).syncAll(eq(draft), anyList());
        assertThat(stored.get()).isNull();
    }

    @Test
    void endingTheSessionDropsItWithoutRestoringTheSnapshotSoTheNextStartBeginsFromScratch() {
        add(tour(draft, 1, 1, 3, "2026-05-06"));
        service.start(draft.getId());

        service.endSession(draft.getId());

        assertThat(stored.get()).isNull();
        assertThat(service.state(draft.getId()).active()).isFalse();
        verify(jdbcTemplate, never()).update(anyString(), eq(SNAPSHOT));
        service.start(draft.getId());
        verify(draftService, times(2)).autoAssignDrivers(draft.getId());
    }

    @Test
    void endingASessionThatDoesNotExistIsHarmless() {
        service.endSession(draft.getId());

        assertThat(service.state(draft.getId()).active()).isFalse();
    }

    @Test
    void failsToResumeOrRecalculateWithoutASession() {
        UUID id = draft.getId();

        assertThatThrownBy(() -> service.recalculate(id)).isInstanceOf(TransportException.class);
        assertThatThrownBy(() -> service.finish(id)).isInstanceOf(TransportException.class);
        assertThatThrownBy(() -> service.discard(id)).isInstanceOf(TransportException.class);
    }
}
