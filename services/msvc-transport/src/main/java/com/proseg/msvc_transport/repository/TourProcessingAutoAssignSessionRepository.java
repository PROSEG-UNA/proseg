package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingAutoAssignSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourProcessingAutoAssignSessionRepository extends JpaRepository<TourProcessingAutoAssignSession, UUID> {

    Optional<TourProcessingAutoAssignSession> findByDraftId(UUID draftId);
}
