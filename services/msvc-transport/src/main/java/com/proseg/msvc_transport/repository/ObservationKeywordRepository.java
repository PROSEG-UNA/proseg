package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.ObservationKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservationKeywordRepository extends JpaRepository<ObservationKeyword, UUID>, JpaSpecificationExecutor<ObservationKeyword> {

    boolean existsByNormalizedText(String normalizedText);

    boolean existsByNormalizedTextAndIdNot(String normalizedText, UUID id);

    Optional<ObservationKeyword> findByNormalizedText(String normalizedText);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update observation_keyword_table
            set is_deleted = false,
                text = :text,
                updated_at = current_timestamp
            where normalized_text = :normalized
              and is_deleted = true
            """, nativeQuery = true)
    int restoreDeleted(@Param("text") String text, @Param("normalized") String normalized);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            delete from observation_keyword_table
            where normalized_text = :normalized
              and is_deleted = true
              and id <> :id
            """, nativeQuery = true)
    void deleteDeletedByNormalizedText(@Param("normalized") String normalized, @Param("id") UUID id);
}
