package com.proseg.msvc_transport.controller;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.util.ApiResponseBuilder;
import com.proseg.msvc_transport.dto.report.DriverOvertimeReportDto;
import com.proseg.msvc_transport.dto.report.FinalCommissionReportDto;
import com.proseg.msvc_transport.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${routes.reports:/api/v1/transport/reports}")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overtime")
    public ResponseEntity<ApiResponse<List<DriverOvertimeReportDto>>> overtime(@RequestParam int year, @RequestParam int month) {
        return ApiResponseBuilder.ok(reportService.overtimeReport(year, month), "Reporte de horas extra generado");
    }

    @GetMapping("/overtime/export")
    public ResponseEntity<String> overtimeExport(@RequestParam int year, @RequestParam int month) {
        List<DriverOvertimeReportDto> rows = reportService.overtimeReport(year, month);
        StringBuilder csv = new StringBuilder("driverId,driverName,ordinaryHours,surplusHours,overtimeHours,totalAssignedTours\\n");
        for (DriverOvertimeReportDto row : rows) {
            csv.append(row.getDriverId()).append(',').append('"').append(row.getDriverName()).append('"').append(',').append(row.getOrdinaryHours()).append(',').append(row.getSurplusHours()).append(',').append(row.getOvertimeHours()).append(',').append(row.getTotalAssignedTours()).append("\\n");
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overtime-report.csv").contentType(MediaType.parseMediaType("text/csv")).body(csv.toString());
    }

    @GetMapping("/final-commission")
    public ResponseEntity<ApiResponse<FinalCommissionReportDto>> finalCommission(@RequestParam int year, @RequestParam int month) {
        return ApiResponseBuilder.ok(reportService.finalCommissionReport(year, month), "Reporte final de comisión generado");
    }

    @GetMapping("/final-commission/export")
    public ResponseEntity<String> finalCommissionExport(@RequestParam int year, @RequestParam int month) {
        FinalCommissionReportDto report = reportService.finalCommissionReport(year, month);
        StringBuilder csv = new StringBuilder();
        csv.append("totalTours,assignedTours,rejectedTours,jornadas,totalSurplusHours,totalOvertimeHours,contractingCount,loansCount\\n");
        csv.append(report.getTotalTours()).append(',').append(report.getAssignedTours()).append(',').append(report.getRejectedTours()).append(',').append(report.getJornadas()).append(',').append(report.getTotalSurplusHours()).append(',').append(report.getTotalOvertimeHours()).append(',').append(report.getContractingCount()).append(',').append(report.getLoansCount()).append("\\n");
        csv.append("\\nrejectedTourId,rejectedTourName,reason\\n");
        report.getRejectedToursList().forEach(item -> csv.append(item.getTourId()).append(',').append('"').append(item.getTourName()).append('"').append(',').append('"').append(item.getReason() != null ? item.getReason().replace("\"", "''") : "").append('"').append("\\n"));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=final-commission-report.csv").contentType(MediaType.parseMediaType("text/csv")).body(csv.toString());
    }
}

