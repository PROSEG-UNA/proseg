package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.Assignment;
import com.proseg.msvc_transport.repository.projection.AssignmentOrphanProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID>, JpaSpecificationExecutor<Assignment> {

    Optional<Assignment> findByTourId(UUID tourId);

    @Query("""
        select case when count(a) > 0 then true else false end
        from Assignment a
        where a.vehicle.id = :vehicleId
          and a.status in :statuses
          and a.tour.startDate < :endDate
          and a.tour.endDate > :startDate
          and (:excludeTourId is null or a.tour.id <> :excludeTourId)
    """)
    boolean existsVehicleOverlap(
            @Param("vehicleId") UUID vehicleId,
            @Param("statuses") Collection<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludeTourId") UUID excludeTourId
    );

    @Query("""
        select a from Assignment a
        where a.driver.id = :driverId
          and a.status in :statuses
          and a.tour.startDate < :endDate
          and a.tour.endDate > :startDate
    """)
    List<Assignment> findDriverAssignmentsOverlapping(
            @Param("driverId") UUID driverId,
            @Param("statuses") Collection<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        select a from Assignment a
        where a.status in :statuses
          and a.tour.startDate >= :startDate
          and a.tour.startDate < :endDate
    """)
    List<Assignment> findAllByStatusAndMonth(
            @Param("statuses") Collection<String> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        select a from Assignment a
        where a.tour.status in :tourStatuses
    """)
    List<Assignment> findAllByTourStatuses(@Param("tourStatuses") Collection<String> tourStatuses);

    long countByDriverId(UUID driverId);

    long countByVehicleId(UUID vehicleId);

    long countByTourId(UUID tourId);

    @Query(value = """
        select
            a.id as assignmentId,
            a.driver_id as driverId,
            a.vehicle_id as vehicleId,
            a.tour_id as tourId,
            (a.driver_id is not null and d.id is null) as missingDriver,
            (a.vehicle_id is not null and v.id is null) as missingVehicle,
            (a.tour_id is not null and t.id is null) as missingTour
        from assignment_table a
        left join driver_table d on d.id = a.driver_id and d.is_deleted = false
        left join vehicle_table v on v.id = a.vehicle_id and v.is_deleted = false
        left join tour_table t on t.id = a.tour_id and t.is_deleted = false
        where a.is_deleted = false
          and (
              (a.driver_id is not null and d.id is null)
              or (a.vehicle_id is not null and v.id is null)
              or (a.tour_id is not null and t.id is null)
          )
        order by a.created_at asc
    """, nativeQuery = true)
    List<AssignmentOrphanProjection> findOrphanReferences();

    @Modifying
    @Query(value = """
        update assignment_table a
        set driver_id = null
        where a.is_deleted = false
          and a.driver_id is not null
          and not exists (
              select 1
              from driver_table d
              where d.id = a.driver_id
                and d.is_deleted = false
          )
    """, nativeQuery = true)
    int clearMissingDriverReferences();

    @Modifying
    @Query(value = """
        update assignment_table a
        set vehicle_id = null
        where a.is_deleted = false
          and a.vehicle_id is not null
          and not exists (
              select 1
              from vehicle_table v
              where v.id = a.vehicle_id
                and v.is_deleted = false
          )
    """, nativeQuery = true)
    int clearMissingVehicleReferences();

    @Modifying
    @Query(value = """
        update assignment_table a
        set is_deleted = true, updated_at = now()
        where a.is_deleted = false
          and a.tour_id is not null
          and not exists (
              select 1
              from tour_table t
              where t.id = a.tour_id
                and t.is_deleted = false
          )
    """, nativeQuery = true)
    int softDeleteAssignmentsWithMissingTour();
}
