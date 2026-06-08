package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.MaintenanceRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceRegisterRepository extends JpaRepository<MaintenanceRegister, UUID> {

    Optional<MaintenanceRegister> findByMaintenanceRequestId(UUID maintenanceRequestId);
}
