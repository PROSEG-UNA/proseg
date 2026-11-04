package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingDraftRowIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

public interface TourProcessingDraftRowIndexRepository extends JpaRepository<TourProcessingDraftRowIndex, UUID>, JpaSpecificationExecutor<TourProcessingDraftRowIndex> {
}
