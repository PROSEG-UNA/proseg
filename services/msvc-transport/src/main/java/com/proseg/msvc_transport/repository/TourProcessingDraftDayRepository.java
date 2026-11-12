package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingDraftDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourProcessingDraftDayRepository extends JpaRepository<TourProcessingDraftDay, UUID> {

    interface DriverTotals {
        UUID getDriverId();
        double getOrdinaryHours();
        double getSurplusHours();
        double getOvertimeHours();
    }

    interface MonthlyTotals extends DriverTotals {
        int getWeekendsWorked();
    }

    @Query(value = """
            select a.driver_id as "driverId", sum(d.ordinary_hours) as "ordinaryHours",
                   sum(d.surplus_hours) as "surplusHours", sum(d.overtime_hours) as "overtimeHours",
                   count(distinct (d.date - (cast(extract(isodow from d.date) as integer) - 6))) filter (
                       where extract(isodow from d.date) >= 6 and d.date between :startDate and :endDate
                         and d.ordinary_hours + d.surplus_hours + d.overtime_hours > 0.000000001
                   ) as "weekendsWorked"
            from tour_processing_draft_day_table d
            join tour_processing_draft_assignment_table a on a.id = d.assignment_id
            join tour_processing_draft_tour_table t on t.id = a.tour_id
            where t.draft_id = :draftId and t.duplicate_rank = 1 and t.approval_status = 'APPROVED'
              and a.driver_id is not null
              and a.resource_type = 'INTERNAL'
              and exists (
                  select 1 from driver_table dr
                  where dr.id = a.driver_id
                    and dr.organizational_unit = 'GENERAL_SERVICES'
              )
            group by a.driver_id
            """, nativeQuery = true)
    List<MonthlyTotals> summarizeMonth(@Param("draftId") UUID draftId,
                                      @Param("startDate") java.time.LocalDate startDate,
                                      @Param("endDate") java.time.LocalDate endDate);

    @Query(value = """
            select a.driver_id as "driverId", sum(d.ordinary_hours) as "ordinaryHours",
                   sum(d.surplus_hours) as "surplusHours", sum(d.overtime_hours) as "overtimeHours"
            from tour_processing_draft_day_table d
            join tour_processing_draft_assignment_table a on a.id = d.assignment_id
            join tour_processing_draft_tour_table t on t.id = a.tour_id
            where t.draft_id = :draftId and t.duplicate_rank = 1 and t.approval_status = 'APPROVED'
              and a.driver_id is not null
              and a.resource_type = 'INTERNAL'
              and exists (
                  select 1 from driver_table dr
                  where dr.id = a.driver_id
                    and dr.organizational_unit = 'GENERAL_SERVICES'
              )
            group by a.driver_id
            """, nativeQuery = true)
    List<DriverTotals> summarizeByDriver(@Param("draftId") UUID draftId);

    @Query("""
            select d from TourProcessingDraftDay d join fetch d.assignment a join fetch a.tour t left join fetch a.borrowedVehicle
            where t.draft.id = :draftId and d.date = :date and t.duplicateRank = 1 and t.approvalStatus = 'APPROVED'
              and a.driverId is not null
            """)
    List<TourProcessingDraftDay> findByDraftIdAndDate(@Param("draftId") UUID draftId, @Param("date") java.time.LocalDate date);

    List<TourProcessingDraftDay> findByAssignmentIdIn(Collection<UUID> assignmentIds);

    @Query("select d from TourProcessingDraftDay d join fetch d.assignment a where a.tour.draft.id = :draftId")
    List<TourProcessingDraftDay> findAllByDraftId(@Param("draftId") UUID draftId);

    @Query("select d from TourProcessingDraftDay d join fetch d.assignment a join fetch a.tour t where d.id = :id and t.draft.id = :draftId")
    Optional<TourProcessingDraftDay> findByIdAndDraftId(@Param("id") UUID id, @Param("draftId") UUID draftId);
}
