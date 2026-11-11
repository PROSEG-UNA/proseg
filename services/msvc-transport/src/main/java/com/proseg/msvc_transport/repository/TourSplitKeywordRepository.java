package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.dto.tourprocessing.TourSplitMode;
import com.proseg.msvc_transport.entity.TourSplitKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourSplitKeywordRepository extends JpaRepository<TourSplitKeyword, UUID> {

    List<TourSplitKeyword> findAllByModeOrderByTextAsc(TourSplitMode mode);

    boolean existsByNormalizedText(String normalizedText);

    boolean existsByNormalizedTextAndIdNot(String normalizedText, UUID id);
}
