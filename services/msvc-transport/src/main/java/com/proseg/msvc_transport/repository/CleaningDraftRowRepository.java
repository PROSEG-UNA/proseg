package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.CleaningDraftRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CleaningDraftRowRepository extends JpaRepository<CleaningDraftRow, UUID>, JpaSpecificationExecutor<CleaningDraftRow> {

    List<CleaningDraftRow> findAllByDraftIdOrderByRowNumberAsc(UUID draftId);

    Optional<CleaningDraftRow> findByIdAndDraftId(UUID id, UUID draftId);
}
