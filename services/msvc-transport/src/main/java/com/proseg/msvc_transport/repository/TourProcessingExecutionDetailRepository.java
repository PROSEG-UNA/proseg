package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.TourProcessingExecutionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourProcessingExecutionDetailRepository extends JpaRepository<TourProcessingExecutionDetail, UUID> {

    List<TourProcessingExecutionDetail> findAllByTourProcessingExecutionIdOrderByOriginalRowNumberAscIdAsc(UUID tourProcessingExecutionId);
}
