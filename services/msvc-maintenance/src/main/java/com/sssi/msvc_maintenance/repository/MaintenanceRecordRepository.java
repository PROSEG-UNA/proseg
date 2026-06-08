package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID>, JpaSpecificationExecutor<MaintenanceRecord> {

    Page<MaintenanceRecord> findByMaintenanceRegisterId(UUID maintenanceRegisterId, Pageable pageable);

    Page<MaintenanceRecord> findByMaintenanceRegisterIdAndAssetId(UUID maintenanceRegisterId, UUID assetId, Pageable pageable);

    Page<MaintenanceRecord> findByAssetIdOrderByCreatedAtDesc(UUID assetId, Pageable pageable);
}
