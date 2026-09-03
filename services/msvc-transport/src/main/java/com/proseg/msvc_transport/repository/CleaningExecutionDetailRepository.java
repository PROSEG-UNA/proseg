package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.CleaningExecutionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CleaningExecutionDetailRepository extends JpaRepository<CleaningExecutionDetail, UUID> {

    List<CleaningExecutionDetail> findAllByCleaningExecutionIdOrderByOriginalRowNumberAscIdAsc(UUID cleaningExecutionId);
}
