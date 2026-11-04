package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingDraft;
import com.proseg.msvc_transport.entity.TourProcessingDraftStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourProcessingDraftRepository extends JpaRepository<TourProcessingDraft, UUID>, JpaSpecificationExecutor<TourProcessingDraft> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from TourProcessingDraft d where d.id = :id")
    Optional<TourProcessingDraft> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByYearAndMonthAndStatus(Integer year, Integer month, TourProcessingDraftStatus status);

    Optional<TourProcessingDraft> findFirstByYearAndMonthOrderByImportedAtDesc(Integer year, Integer month);
}
