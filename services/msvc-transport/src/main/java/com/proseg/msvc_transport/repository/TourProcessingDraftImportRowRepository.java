package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingDraftImportRow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TourProcessingDraftImportRowRepository extends JpaRepository<TourProcessingDraftImportRow, UUID> {
    List<TourProcessingDraftImportRow> findAllByDraftId(UUID draftId);
}
