package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID>, JpaSpecificationExecutor<MaintenanceRequest> {

    Page<MaintenanceRequest> findByCompanyId(UUID companyId, Pageable pageable);

    boolean existsByCompanyId(UUID companyId);
}

