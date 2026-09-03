package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.AssignmentRequestDto;
import com.proseg.msvc_transport.dto.response.AssignmentGenerateResponseDto;
import com.proseg.msvc_transport.dto.response.AssignmentIntegrityCleanupResponseDto;
import com.proseg.msvc_transport.dto.response.AssignmentIntegrityReportDto;
import com.proseg.msvc_transport.dto.response.AssignmentOrphanReferenceDto;
import com.proseg.msvc_transport.dto.response.AssignmentResponseDto;
import com.proseg.msvc_transport.entity.Assignment;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.Tour;
import com.proseg.msvc_transport.entity.Vehicle;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.AssignmentRepository;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.repository.TourRepository;
import com.proseg.msvc_transport.repository.VehicleMaintenanceRepository;
import com.proseg.msvc_transport.repository.VehicleRepository;
import com.proseg.msvc_transport.repository.projection.AssignmentOrphanProjection;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.*;
import java.util.*;
import org.hibernate.ObjectNotFoundException;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private static final Set<String> ACTIVE_ASSIGNMENT_STATUSES = Set.of("ASSIGNED", "CONTRACTED", "PENDING");
    private static final Set<String> ACTIVE_MAINTENANCE_STATUSES = Set.of("PLANNED", "IN_PROGRESS");
    private static final double MAX_HOURS_PER_DAY = 12D;
    private static final double MONTH_ORDINARY_LIMIT = 160D;
    private static final double MONTH_SURPLUS_LIMIT = 200D;
    private static final double MONTH_OVERTIME_LIMIT = 240D;

    private final AssignmentRepository assignmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final TourRepository tourRepository;

    @Transactional
    public AssignmentResponseDto create(AssignmentRequestDto request) {
        Tour tour = findTour(request.getTourId());
        Driver driver = findDriverNullable(request.getDriverId());
        Vehicle vehicle = findVehicleNullable(request.getVehicleId());
        validateBusinessRules(tour, driver, vehicle, request.isContracted(), request.getTourId());

        Assignment assignment = assignmentRepository.findByTourId(tour.getId()).orElse(new Assignment());
        assignment.setTour(tour);
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setStatus(request.getStatus());
        assignment.setNotes(request.getNotes());
        assignment.setContracted(request.isContracted());
        assignment.setRejectionReason(request.getRejectionReason());
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto findById(UUID id) {
        return assignmentRepository.findById(id).map(this::toResponse).orElseThrow(() -> TransportException.notFound("Asignación", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<AssignmentResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Assignment> spec = Specification.where(GenericSpecifications.withSearch(Assignment.class, search)).and(GenericSpecifications.withColumnFilters(Assignment.class, filters));
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(Assignment.class, pageable.getSort()));
        return assignmentRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    @Transactional
    public AssignmentResponseDto update(UUID id, AssignmentRequestDto request) {
        Assignment assignment = assignmentRepository.findById(id).orElseThrow(() -> TransportException.notFound("Asignación", id.toString()));
        Tour tour = findTour(request.getTourId());
        Driver driver = findDriverNullable(request.getDriverId());
        Vehicle vehicle = findVehicleNullable(request.getVehicleId());
        validateBusinessRules(tour, driver, vehicle, request.isContracted(), tour.getId());

        assignment.setTour(tour);
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setStatus(request.getStatus());
        assignment.setNotes(request.getNotes());
        assignment.setContracted(request.isContracted());
        assignment.setRejectionReason(request.getRejectionReason());
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void delete(UUID id) {
        Assignment assignment = assignmentRepository.findById(id).orElseThrow(() -> TransportException.notFound("Asignación", id.toString()));
        assignmentRepository.delete(assignment);
    }

    @Transactional
    public AssignmentGenerateResponseDto generateAssignments() {
        List<Assignment> existing = assignmentRepository.findAllByTourStatuses(Set.of("PLANNED", "PENDING"));
        if (!existing.isEmpty()) assignmentRepository.deleteAll(existing);

        List<Tour> allTours = tourRepository.findAllByStatusIn(Set.of("PLANNED", "PENDING"));
        allTours.sort(Comparator.comparing(Tour::getStartDate));

        List<Tour> g1 = allTours.stream().filter(t -> t.getPriority() == 1 && t.getPassengers() < 5).toList();
        List<Tour> g2 = allTours.stream().filter(t -> t.getPriority() == 6 || t.getPriority() == 7).toList();
        List<Tour> g3 = allTours.stream().filter(t -> t.getPriority() == 1 && t.getPassengers() >= 5).toList();
        List<Tour> g5 = allTours.stream().filter(t -> !(g1.contains(t) || g2.contains(t) || g3.contains(t))).toList();

        List<Tour> forContracting = new ArrayList<>();
        int assigned = assignBatch(g1, forContracting, false);
        assigned += assignBatch(g2, forContracting, false);
        assigned += assignBatch(g3, forContracting, false);
        int contracted = processContracting(forContracting);
        assigned += assignBatch(g5, new ArrayList<>(), true);

        int rejected = (int) assignmentRepository.findAll().stream().filter(a -> "REJECTED".equalsIgnoreCase(a.getStatus())).count();
        return AssignmentGenerateResponseDto.builder().totalToursProcessed(allTours.size()).assignedCount(assigned).contractedCount(contracted).rejectedCount(rejected).build();
    }

    @Transactional(readOnly = true)
    public AssignmentIntegrityReportDto getIntegrityReport() {
        List<AssignmentOrphanProjection> orphans = assignmentRepository.findOrphanReferences();
        int missingDriverCount = 0;
        int missingVehicleCount = 0;
        int missingTourCount = 0;

        List<AssignmentOrphanReferenceDto> rows = new ArrayList<>();
        for (AssignmentOrphanProjection orphan : orphans) {
            if (orphan.getMissingDriver()) missingDriverCount++;
            if (orphan.getMissingVehicle()) missingVehicleCount++;
            if (orphan.getMissingTour()) missingTourCount++;

            rows.add(AssignmentOrphanReferenceDto.builder()
                    .assignmentId(orphan.getAssignmentId())
                    .driverId(orphan.getDriverId())
                    .vehicleId(orphan.getVehicleId())
                    .tourId(orphan.getTourId())
                    .missingDriver(orphan.getMissingDriver())
                    .missingVehicle(orphan.getMissingVehicle())
                    .missingTour(orphan.getMissingTour())
                    .build());
        }

        return AssignmentIntegrityReportDto.builder()
                .totalOrphans(rows.size())
                .missingDriverCount(missingDriverCount)
                .missingVehicleCount(missingVehicleCount)
                .missingTourCount(missingTourCount)
                .rows(rows)
                .build();
    }

    @Transactional
    public AssignmentIntegrityCleanupResponseDto cleanupOrphanReferences() {
        int clearedDriverReferences = assignmentRepository.clearMissingDriverReferences();
        int clearedVehicleReferences = assignmentRepository.clearMissingVehicleReferences();
        int softDeletedMissingTourAssignments = assignmentRepository.softDeleteAssignmentsWithMissingTour();

        return AssignmentIntegrityCleanupResponseDto.builder()
                .clearedDriverReferences(clearedDriverReferences)
                .clearedVehicleReferences(clearedVehicleReferences)
                .softDeletedMissingTourAssignments(softDeletedMissingTourAssignments)
                .build();
    }

    private int assignBatch(List<Tour> tours, List<Tour> forContracting, boolean rejectIfNoDriver) {
        int assigned = 0;
        for (Tour tour : tours) {
            Optional<Assignment> result = tryAssignTour(tour);
            if (result.isPresent()) {
                Assignment saved = assignmentRepository.save(result.get());
                if ("ASSIGNED".equalsIgnoreCase(saved.getStatus())) assigned++;
                continue;
            }
            if (!rejectIfNoDriver && tour.getPassengers() >= 5) {
                forContracting.add(tour);
            } else {
                saveRejected(tour, tour.getPassengers() < 5 ? "No hay chofer disponible y no aplica contratación para <5 pasajeros" : "No hay recursos disponibles");
            }
        }
        return assigned;
    }

    private int processContracting(List<Tour> tours) {
        int contracted = 0;
        for (Tour tour : tours) {
            if (tour.getPassengers() < 5) {
                saveRejected(tour, "No se puede contratar para giras con menos de 5 pasajeros");
                continue;
            }
            Assignment assignment = new Assignment();
            assignment.setTour(tour);
            assignment.setStatus("CONTRACTED");
            assignment.setContracted(true);
            assignment.setNotes("Asignación por contratación");
            assignmentRepository.save(assignment);
            tour.setStatus("ASSIGNED");
            tourRepository.save(tour);
            contracted++;
        }
        return contracted;
    }

    private Optional<Assignment> tryAssignTour(Tour tour) {
        List<Driver> drivers = driverRepository.findAllByStatusIgnoreCaseAndAvailabilityTrue("ACTIVE");
        List<Vehicle> vehicles = vehicleRepository.findAllByStatusIgnoreCaseAndAvailableTrueAndUnderMaintenanceFalse("ACTIVE");
        drivers.sort(Comparator.comparingDouble(Driver::getAccumulatedHours));
        vehicles.sort(Comparator.comparingInt(Vehicle::getCapacity));

        for (Driver driver : drivers) {
            for (Vehicle vehicle : vehicles) {
                if (isDriverRestricted(driver, vehicle)) continue;
                if (vehicle.getCapacity() < tour.getPassengers()) continue;
                if (vehicleMaintenanceRepository.existsByVehicleIdAndStatusIn(vehicle.getId(), ACTIVE_MAINTENANCE_STATUSES)) continue;
                if (assignmentRepository.existsVehicleOverlap(vehicle.getId(), ACTIVE_ASSIGNMENT_STATUSES, tour.getStartDate(), tour.getEndDate(), tour.getId())) continue;
                if (!isDriverAvailable(driver, tour)) continue;

                Assignment assignment = new Assignment();
                assignment.setTour(tour);
                assignment.setDriver(driver);
                assignment.setVehicle(vehicle);
                assignment.setStatus("ASSIGNED");
                assignment.setContracted(false);
                tour.setStatus("ASSIGNED");
                tourRepository.save(tour);
                updateDriverHours(driver, tour);
                return Optional.of(assignment);
            }
        }
        return Optional.empty();
    }

    private boolean isDriverAvailable(Driver driver, Tour tour) {
        List<Assignment> overlapping = assignmentRepository.findDriverAssignmentsOverlapping(driver.getId(), ACTIVE_ASSIGNMENT_STATUSES, tour.getStartDate(), tour.getEndDate());
        if (!overlapping.isEmpty()) return false;

        double tourHours = calculateHours(tour);
        if (!withinDailyLimit(driver, tour, tourHours)) return false;

        double projectedHours = driver.getAccumulatedHours() + tourHours;
        if (projectedHours > MONTH_OVERTIME_LIMIT) return false;

        return keepsOneFreeWeekend(driver, tour);
    }

    private boolean withinDailyLimit(Driver driver, Tour tour, double tourHours) {
        LocalDate current = tour.getStartDate().toLocalDate();
        LocalDate end = tour.getEndDate().toLocalDate();
        Map<LocalDate, Double> dayHours = new HashMap<>();

        LocalDateTime monthStart = current.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = current.withDayOfMonth(current.lengthOfMonth()).atTime(23, 59, 59);
        List<Assignment> monthAssignments = assignmentRepository.findDriverAssignmentsOverlapping(driver.getId(), ACTIVE_ASSIGNMENT_STATUSES, monthStart, monthEnd);
        for (Assignment a : monthAssignments) {
            Tour assignmentTour = safeTour(a);
            if (assignmentTour == null) continue;
            LocalDate date = assignmentTour.getStartDate().toLocalDate();
            dayHours.merge(date, calculateHours(assignmentTour), Double::sum);
        }

        int days = Math.max(1, (int) Duration.between(tour.getStartDate(), tour.getEndDate()).toDays() + 1);
        double perDay = tourHours / days;
        while (!current.isAfter(end)) {
            double existing = dayHours.getOrDefault(current, 0D);
            if (existing + perDay > MAX_HOURS_PER_DAY) return false;
            current = current.plusDays(1);
        }
        return true;
    }

    private boolean keepsOneFreeWeekend(Driver driver, Tour tour) {
        YearMonth ym = YearMonth.from(tour.getStartDate());
        Set<LocalDate> allWeekendDays = new HashSet<>();
        LocalDate d = ym.atDay(1);
        while (!d.isAfter(ym.atEndOfMonth())) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) allWeekendDays.add(d);
            d = d.plusDays(1);
        }
        if (allWeekendDays.isEmpty()) return true;

        List<Assignment> monthAssignments = assignmentRepository.findDriverAssignmentsOverlapping(driver.getId(), ACTIVE_ASSIGNMENT_STATUSES, ym.atDay(1).atStartOfDay(), ym.atEndOfMonth().atTime(23, 59, 59));
        Set<LocalDate> occupiedWeekendDays = new HashSet<>();
        for (Assignment a : monthAssignments) {
            Tour assignmentTour = safeTour(a);
            if (assignmentTour == null) continue;
            addWeekendRange(occupiedWeekendDays, assignmentTour.getStartDate().toLocalDate(), assignmentTour.getEndDate().toLocalDate());
        }
        addWeekendRange(occupiedWeekendDays, tour.getStartDate().toLocalDate(), tour.getEndDate().toLocalDate());
        return occupiedWeekendDays.size() < allWeekendDays.size();
    }

    private void addWeekendRange(Set<LocalDate> set, LocalDate start, LocalDate end) {
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) set.add(d);
            d = d.plusDays(1);
        }
    }

    private void updateDriverHours(Driver driver, Tour tour) {
        double duration = calculateHours(tour);
        double accumulated = driver.getAccumulatedHours() + duration;
        driver.setAccumulatedHours(accumulated);
        double surplus = Math.max(0D, Math.min(accumulated, MONTH_SURPLUS_LIMIT) - MONTH_ORDINARY_LIMIT);
        double overtime = Math.max(0D, accumulated - MONTH_SURPLUS_LIMIT);
        driver.setSurplusHours(surplus);
        driver.setOvertimeHours(overtime);
        driverRepository.save(driver);
    }

    private void validateBusinessRules(Tour tour, Driver driver, Vehicle vehicle, boolean contracted, UUID tourId) {
        if (!contracted) {
            if (driver == null || vehicle == null) {
                throw TransportException.badRequest("ASSIGNMENT_INVALID", "Chofer y vehículo son obligatorios en asignación no contratada");
            }
            if (vehicle.getCapacity() < tour.getPassengers()) {
                throw TransportException.badRequest("VEHICLE_CAPACITY", "La capacidad del vehículo es menor a los pasajeros de la gira");
            }
            if (vehicle.isUnderMaintenance() || vehicleMaintenanceRepository.existsByVehicleIdAndStatusIn(vehicle.getId(), ACTIVE_MAINTENANCE_STATUSES)) {
                throw TransportException.badRequest("VEHICLE_IN_MAINTENANCE", "El vehículo está en mantenimiento activo");
            }
            if (assignmentRepository.existsVehicleOverlap(vehicle.getId(), ACTIVE_ASSIGNMENT_STATUSES, tour.getStartDate(), tour.getEndDate(), tourId)) {
                throw TransportException.badRequest("VEHICLE_OVERLAP", "El vehículo ya está asignado a una gira en ese rango");
            }
        } else if (tour.getPassengers() < 5) {
            throw TransportException.badRequest("CONTRACTING_NOT_ALLOWED", "No se permite contratación para giras de menos de 5 pasajeros");
        }
    }

    private boolean isDriverRestricted(Driver driver, Vehicle vehicle) {
        if (driver.getRestrictions() == null || driver.getRestrictions().isBlank() || vehicle.getType() == null) return false;
        return driver.getRestrictions().toLowerCase().contains(vehicle.getType().toLowerCase());
    }

    private Driver findDriverNullable(UUID id) {
        if (id == null) return null;
        return driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
    }

    private Vehicle findVehicleNullable(UUID id) {
        if (id == null) return null;
        return vehicleRepository.findById(id).orElseThrow(() -> TransportException.notFound("Vehículo", id.toString()));
    }

    private Tour findTour(UUID id) {
        return tourRepository.findById(id).orElseThrow(() -> TransportException.notFound("Gira", id.toString()));
    }

    private void saveRejected(Tour tour, String reason) {
        Assignment rejected = new Assignment();
        rejected.setTour(tour);
        rejected.setStatus("REJECTED");
        rejected.setContracted(false);
        rejected.setRejectionReason(reason);
        assignmentRepository.save(rejected);
        tour.setStatus("REJECTED");
        tourRepository.save(tour);
    }

    private double calculateHours(Tour tour) {
        long minutes = Math.max(0L, Duration.between(tour.getStartDate(), tour.getEndDate()).toMinutes());
        return Math.max(1D, minutes / 60.0D);
    }

    public AssignmentResponseDto toResponse(Assignment entity) {
        Driver driver = safeDriver(entity);
        Vehicle vehicle = safeVehicle(entity);
        Tour tour = safeTour(entity);

        String driverName = null;
        if (driver != null) {
            driverName = (safeText(driver.getFirstName()) + " " + safeText(driver.getLastName())).trim();
            if (driverName.isBlank()) {
                driverName = null;
            }
        }

        return AssignmentResponseDto.builder()
                .id(entity.getId())
                .driverId(driver != null ? driver.getId() : null)
                .driverName(driverName)
                .vehicleId(vehicle != null ? vehicle.getId() : null)
                .vehiclePlate(vehicle != null ? vehicle.getPlate() : null)
                .tourId(tour != null ? tour.getId() : null)
                .tourName(tour != null ? tour.getName() : "Gira no disponible")
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .isContracted(entity.isContracted())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Driver safeDriver(Assignment entity) {
        try {
            return entity.getDriver();
        } catch (EntityNotFoundException | ObjectNotFoundException ex) {
            return null;
        }
    }

    private Vehicle safeVehicle(Assignment entity) {
        try {
            return entity.getVehicle();
        } catch (EntityNotFoundException | ObjectNotFoundException ex) {
            return null;
        }
    }

    private Tour safeTour(Assignment entity) {
        try {
            return entity.getTour();
        } catch (EntityNotFoundException | ObjectNotFoundException ex) {
            return null;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
