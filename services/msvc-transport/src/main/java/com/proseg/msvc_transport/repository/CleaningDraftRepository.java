package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.CleaningDraft;
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
public interface CleaningDraftRepository extends JpaRepository<CleaningDraft, UUID>, JpaSpecificationExecutor<CleaningDraft> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CleaningDraft d where d.id = :id")
    Optional<CleaningDraft> findByIdForUpdate(@Param("id") UUID id);
}
