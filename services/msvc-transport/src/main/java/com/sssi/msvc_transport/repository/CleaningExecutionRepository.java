package com.sssi.msvc_transport.repository;

import com.sssi.msvc_transport.entity.CleaningExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CleaningExecutionRepository extends JpaRepository<CleaningExecution, UUID>, JpaSpecificationExecutor<CleaningExecution> {
}
