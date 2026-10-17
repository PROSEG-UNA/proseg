package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.CleaningDraftRowDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CleaningDraftRowDayRepository extends JpaRepository<CleaningDraftRowDay, UUID> {

    List<CleaningDraftRowDay> findByRowIdOrderByDateAsc(UUID rowId);

    List<CleaningDraftRowDay> findByRowIdIn(Collection<UUID> rowIds);

    Optional<CleaningDraftRowDay> findByIdAndRowId(UUID id, UUID rowId);

    void deleteByRowIdIn(Collection<UUID> rowIds);
}
