package com.sssi.msvc_transport.service;

import com.sssi.msvc_transport.dto.report.DriverOvertimeReportDto;
import com.sssi.msvc_transport.dto.report.FinalCommissionRejectedTourDto;
import com.sssi.msvc_transport.dto.report.FinalCommissionReportDto;
import com.sssi.msvc_transport.entity.Assignment;
import com.sssi.msvc_transport.entity.Driver;
import com.sssi.msvc_transport.entity.Tour;
import com.sssi.msvc_transport.repository.AssignmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final double MONTH_ORDINARY_LIMIT = 160D;
    private static final double MONTH_SURPLUS_LIMIT = 200D;

    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<DriverOvertimeReportDto> overtimeReport(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<Assignment> assignments = assignmentRepository.findAllByStatusAndMonth(Set.of("ASSIGNED", "CONTRACTED"), ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        Map<String, DriverOvertimeReportDto> byDriver = new HashMap<>();
        for (Assignment assignment : assignments) {
            Driver driver = safeDriver(assignment);
            if (driver == null) continue;
            Tour tour = safeTour(assignment);
            if (tour == null) continue;
            String key = driver.getId().toString();
            DriverOvertimeReportDto dto = byDriver.computeIfAbsent(key, ignored -> DriverOvertimeReportDto.builder().driverId(driver.getId()).driverName(driver.getFirstName() + " " + driver.getLastName()).ordinaryHours(0D).surplusHours(0D).overtimeHours(0D).totalAssignedTours(0).build());
            double hours = calculateHours(tour);
            double currentTotal = dto.getOrdinaryHours() + dto.getSurplusHours() + dto.getOvertimeHours();
            double projected = currentTotal + hours;
            dto.setOrdinaryHours(Math.min(MONTH_ORDINARY_LIMIT, projected));
            dto.setSurplusHours(Math.max(0D, Math.min(projected, MONTH_SURPLUS_LIMIT) - MONTH_ORDINARY_LIMIT));
            dto.setOvertimeHours(Math.max(0D, projected - MONTH_SURPLUS_LIMIT));
            dto.setTotalAssignedTours(dto.getTotalAssignedTours() + 1);
        }
        return byDriver.values().stream().sorted(Comparator.comparing(DriverOvertimeReportDto::getDriverName)).toList();
    }

    @Transactional(readOnly = true)
    public FinalCommissionReportDto finalCommissionReport(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<Assignment> assignments = assignmentRepository.findAllByStatusAndMonth(Set.of("ASSIGNED", "CONTRACTED", "REJECTED"), ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        int total = assignments.size();
        int assigned = 0;
        int rejected = 0;
        int contracted = 0;
        double jornadas = 0D;
        double overtime = 0D;
        double surplus = 0D;
        List<FinalCommissionRejectedTourDto> rejectedTours = new ArrayList<>();
        for (Assignment assignment : assignments) {
            Tour tour = safeTour(assignment);
            if (tour == null) continue;
            if ("ASSIGNED".equalsIgnoreCase(assignment.getStatus())) {
                assigned++;
                jornadas += calculateHours(tour);
            } else if ("CONTRACTED".equalsIgnoreCase(assignment.getStatus())) {
                contracted++;
                jornadas += calculateHours(tour);
            } else if ("REJECTED".equalsIgnoreCase(assignment.getStatus())) {
                rejected++;
                rejectedTours.add(FinalCommissionRejectedTourDto.builder().tourId(tour.getId()).tourName(tour.getName()).reason(assignment.getRejectionReason()).build());
            }
        }
        for (DriverOvertimeReportDto report : overtimeReport(year, month)) {
            overtime += report.getOvertimeHours();
            surplus += report.getSurplusHours();
        }
        return FinalCommissionReportDto.builder().totalTours(total).assignedTours(assigned).rejectedTours(rejected).jornadas(jornadas).totalSurplusHours(surplus).totalOvertimeHours(overtime).contractingCount(contracted).loansCount(0).rejectedToursList(rejectedTours).build();
    }

    private double calculateHours(Tour tour) {
        long minutes = Math.max(0L, Duration.between(tour.getStartDate(), tour.getEndDate()).toMinutes());
        return Math.max(1D, minutes / 60.0D);
    }

    private Driver safeDriver(Assignment assignment) {
        try {
            return assignment.getDriver();
        } catch (EntityNotFoundException | ObjectNotFoundException ex) {
            return null;
        }
    }

    private Tour safeTour(Assignment assignment) {
        try {
            return assignment.getTour();
        } catch (EntityNotFoundException | ObjectNotFoundException ex) {
            return null;
        }
    }
}
