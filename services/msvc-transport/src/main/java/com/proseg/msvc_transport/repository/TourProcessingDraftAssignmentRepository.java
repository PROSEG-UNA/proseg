package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingDraftAssignment;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourProcessingDraftAssignmentRepository extends JpaRepository<TourProcessingDraftAssignment, UUID> {

    @Query(value = """
            select a from TourProcessingDraftAssignment a join fetch a.tour t left join fetch a.borrowedVehicle
            where t.draft.id = :draftId and t.duplicateRank = 1 and t.approvalStatus = 'APPROVED'
              and a.driverId = :driverId and exists (select d.id from TourProcessingDraftDay d where d.assignment = a)
            order by t.departureDate, t.number, a.part, a.id
            """, countQuery = """
            select count(a) from TourProcessingDraftAssignment a join a.tour t
            where t.draft.id = :draftId and t.duplicateRank = 1 and t.approvalStatus = 'APPROVED'
              and a.driverId = :driverId and exists (select d.id from TourProcessingDraftDay d where d.assignment = a)
            """)
    org.springframework.data.domain.Page<TourProcessingDraftAssignment> findDriverPage(
            @Param("draftId") UUID draftId, @Param("driverId") UUID driverId, org.springframework.data.domain.Pageable pageable);

    @Query("""
            select a from TourProcessingDraftAssignment a join fetch a.tour t left join fetch a.borrowedVehicle
            where t.draft.id = :draftId and t.duplicateRank = 1 and t.approvalStatus = 'APPROVED' and a.driverId is not null
            """)
    List<TourProcessingDraftAssignment> findActiveByDraftId(@Param("draftId") UUID draftId);

    @Query("select a from TourProcessingDraftAssignment a join fetch a.tour t join fetch t.draft where a.driverId = :driverId")
    List<TourProcessingDraftAssignment> findAllByDriverId(@Param("driverId") UUID driverId);

    @Query("select a from TourProcessingDraftAssignment a join fetch a.tour t where a.id = :id and t.draft.id = :draftId")
    Optional<TourProcessingDraftAssignment> findByIdAndDraftId(@Param("id") UUID id, @Param("draftId") UUID draftId);

    @Query("select a from TourProcessingDraftAssignment a join fetch a.tour t where t.id in :tourIds order by a.part")
    List<TourProcessingDraftAssignment> findAllByTourIdIn(@Param("tourIds") Collection<UUID> tourIds);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update tour_processing_draft_assignment_table a
               set driver_id = :driverId, driver = :name
              from tour_processing_draft_tour_table t
              join tour_processing_draft_table d on d.id = t.draft_id
             where a.tour_id = t.id
               and d.status = 'DRAFT'
               and a.resource_type <> 'EXTERNAL'
               and a.driver_id is null
               and regexp_replace(lower(trim(a.driver)), '\\s+', ' ', 'g') = :driverKey
            """, nativeQuery = true)
    int linkDriver(@Param("driverId") UUID driverId, @Param("name") String name, @Param("driverKey") String driverKey);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TourProcessingDraftAssignment a set a.driver = :name
             where a.driverId = :driverId
               and a.tour.id in (select t.id from TourProcessingDraftTour t where t.draft.status = :status)
            """)
    int renameDriver(@Param("driverId") UUID driverId, @Param("name") String name, @Param("status") TourProcessingDraftStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TourProcessingDraftAssignment a set a.driverId = null
             where a.driverId = :driverId
               and a.tour.id in (select t.id from TourProcessingDraftTour t where t.draft.status = :status)
            """)
    int unlinkDriver(@Param("driverId") UUID driverId, @Param("status") TourProcessingDraftStatus status);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update tour_processing_draft_assignment_table a
               set vehicle_id = :vehicleId, vehicle = :plate
              from tour_processing_draft_tour_table t
              join tour_processing_draft_table d on d.id = t.draft_id
             where a.tour_id = t.id
               and d.status = 'DRAFT'
               and a.resource_type = 'INTERNAL'
               and a.vehicle_id is null
               and a.vehicle ~ '[0-9]'
               and left(upper(regexp_replace(a.vehicle, '\\s+', '', 'g')), 20) = :plateKey
            """, nativeQuery = true)
    int linkVehicle(@Param("vehicleId") UUID vehicleId, @Param("plate") String plate, @Param("plateKey") String plateKey);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TourProcessingDraftAssignment a set a.vehicle = :plate
             where a.vehicleId = :vehicleId
               and a.tour.id in (select t.id from TourProcessingDraftTour t where t.draft.status = :status)
            """)
    int renameVehicle(@Param("vehicleId") UUID vehicleId, @Param("plate") String plate, @Param("status") TourProcessingDraftStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TourProcessingDraftAssignment a set a.vehicleId = null
             where a.vehicleId = :vehicleId
               and a.tour.id in (select t.id from TourProcessingDraftTour t where t.draft.status = :status)
            """)
    int unlinkVehicle(@Param("vehicleId") UUID vehicleId, @Param("status") TourProcessingDraftStatus status);

    @Query(value = """
            select exists (
                select 1
                  from tour_processing_draft_assignment_table a
                  join tour_processing_draft_tour_table t on t.id = a.tour_id
                  join tour_processing_draft_table d on d.id = t.draft_id
                 where a.borrowed_vehicle_id = :borrowedVehicleId
                   and d.status = 'DRAFT'
                   and not d.is_deleted
            ) or exists (
                select 1
                  from tour_processing_draft_import_row_table r
                  join tour_processing_draft_table d on d.id = r.draft_id
                 cross join lateral jsonb_array_elements(r.payload -> 'assignments') part
                 where part ->> 'borrowedVehicleId' = cast(:borrowedVehicleId as text)
                   and d.status = 'DRAFT'
                   and not d.is_deleted
            )
            """, nativeQuery = true)
    boolean isBorrowedVehicleInOpenDraft(@Param("borrowedVehicleId") UUID borrowedVehicleId);

    @Query("""
            select a from TourProcessingDraftAssignment a
            join fetch a.tour t
            join fetch t.draft d
            where a.vehicleId = :vehicleId
              and d.status = :status
            """)
    List<TourProcessingDraftAssignment> findByVehicleIdAndDraftStatus(
            @Param("vehicleId") UUID vehicleId,
            @Param("status") TourProcessingDraftStatus status);
}
