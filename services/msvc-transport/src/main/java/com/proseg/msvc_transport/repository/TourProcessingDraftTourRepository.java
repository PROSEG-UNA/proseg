package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingDraftTour;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourProcessingDraftTourRepository extends JpaRepository<TourProcessingDraftTour, UUID>, JpaSpecificationExecutor<TourProcessingDraftTour> {

    @EntityGraph(attributePaths = {"assignments", "assignments.borrowedVehicle"})
    List<TourProcessingDraftTour> findAllByDraftIdOrderByRowNumberAsc(UUID draftId);

    @EntityGraph(attributePaths = {"assignments", "assignments.borrowedVehicle"})
    List<TourProcessingDraftTour> findAllByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = "assignments")
    Optional<TourProcessingDraftTour> findByIdAndDraftId(UUID id, UUID draftId);

    @EntityGraph(attributePaths = "assignments")
    @Query("select t from TourProcessingDraftTour t where t.draft.id = :draftId and t.duplicateRank = 1 and lower(t.number) in :numbers")
    List<TourProcessingDraftTour> findPrimariesByNumbers(@Param("draftId") UUID draftId, @Param("numbers") Collection<String> numbers);
}
