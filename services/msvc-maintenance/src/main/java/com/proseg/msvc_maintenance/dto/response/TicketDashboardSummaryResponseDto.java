package com.proseg.msvc_maintenance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDashboardSummaryResponseDto {
    private long totalTickets;
    private long pendingTickets;
    private long overdueTickets;
    private long resolvedTickets;
    private long cancelledTickets;
    private long unassignedTickets;
    private long recentTicketsLast7Days;
    private double resolutionRate;
    private Double averageResolutionHours;
    private List<TicketDashboardMetricItemDto> byStatus;
    private List<TicketDashboardMetricItemDto> byPriority;
    private List<TicketDashboardMetricItemDto> byTechnician;
    private List<TicketDashboardMetricItemDto> byCompany;
}
