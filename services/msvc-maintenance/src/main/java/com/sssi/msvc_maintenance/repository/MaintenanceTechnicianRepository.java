package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.MaintenanceTechnician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenanceTechnicianRepository extends JpaRepository<MaintenanceTechnician, UUID>, JpaSpecificationExecutor<MaintenanceTechnician> {

    Page<MaintenanceTechnician> findByMaintenanceRequestId(UUID maintenanceRequestId, Pageable pageable);

    boolean existsByMaintenanceRequestId(UUID maintenanceRequestId);
}

