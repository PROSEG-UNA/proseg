package com.proseg.msvc_transport.entity;

import com.proseg.common.specification.Filterable;
import com.proseg.common.specification.FilterType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Immutable
@Getter
@Synchronize({"tour_processing_draft_tour_table", "tour_processing_draft_assignment_table", "tour_processing_draft_import_row_table"})
@Subselect("""
        select t.id, t.draft_id, t.row_number, t.number, t.approval_status, a.driver, a.vehicle, t.passengers, t.executing_unit, t.responsible, t.destination, t.duration_days, t.priority, t.modality, t.departure_time, t.return_time, t.departure_date, t.return_date, t.observations from tour_processing_draft_tour_table t left join tour_processing_draft_assignment_table a on a.tour_id = t.id and a.part = 1 union all select r.id, r.draft_id, cast(r.payload->>'rowNumber' as integer) as row_number, cast(r.payload->>'number' as text) as number, coalesce(cast(r.payload->>'approvalStatus' as text), 'APPROVED') as approval_status, cast(r.payload->>'driver' as text) as driver, cast(r.payload->>'vehicle' as text) as vehicle, cast(r.payload->>'passengers' as integer) as passengers, cast(r.payload->>'executingUnit' as text) as executing_unit, cast(r.payload->>'responsible' as text) as responsible, cast(r.payload->>'destination' as text) as destination, cast(r.payload->>'durationDays' as integer) as duration_days, cast(r.payload->>'priority' as integer) as priority, cast(r.payload->>'modality' as text) as modality, cast(r.payload->>'departureTime' as time) as departure_time, cast(r.payload->>'returnTime' as time) as return_time, cast(r.payload->>'departureDate' as date) as departure_date, cast(r.payload->>'returnDate' as date) as return_date, cast(r.payload->>'observations' as text) as observations from tour_processing_draft_import_row_table r
        """)
public class TourProcessingDraftRowIndex {
    @Id
    private UUID id;
    private UUID draftId;
    @Filterable(type = FilterType.TEXT)
    private Integer rowNumber;
    @Filterable(type = FilterType.TEXT)
    private String number;
    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.STRING)
    private TourApprovalStatus approvalStatus;
    @Filterable(type = FilterType.TEXT)
    private String driver;
    @Filterable(type = FilterType.TEXT)
    private String vehicle;
    @Filterable(type = FilterType.TEXT)
    private Integer passengers;
    @Filterable(type = FilterType.TEXT)
    private String executingUnit;
    @Filterable(type = FilterType.TEXT)
    private String responsible;
    @Filterable(type = FilterType.TEXT)
    private String destination;
    @Filterable(type = FilterType.TEXT)
    private Integer durationDays;
    @Filterable(type = FilterType.TEXT)
    private Integer priority;
    @Filterable(type = FilterType.TEXT)
    private String modality;
    @Filterable(type = FilterType.TEXT)
    private LocalTime departureTime;
    @Filterable(type = FilterType.TEXT)
    private LocalTime returnTime;
    @Filterable(type = FilterType.DATE)
    private LocalDate departureDate;
    @Filterable(type = FilterType.DATE)
    private LocalDate returnDate;
    @Filterable(type = FilterType.TEXT)
    private String observations;
}
