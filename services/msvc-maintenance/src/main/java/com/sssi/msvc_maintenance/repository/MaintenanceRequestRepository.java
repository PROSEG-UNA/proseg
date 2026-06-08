package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.MaintenanceRequest;
import com.sssi.msvc_maintenance.entity.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID>, JpaSpecificationExecutor<MaintenanceRequest> {

    Page<MaintenanceRequest> findByCompanyId(UUID companyId, Pageable pageable);

    boolean existsByCompanyId(UUID companyId);

    Page<MaintenanceRequest> findByAssignedTechnicians_KeycloakUserIdAndStatus(
            String keycloakUserId, MaintenanceStatus status, Pageable pageable);

    Page<MaintenanceRequest> findByAssignedTechnicians_KeycloakUserId(
            String keycloakUserId, Pageable pageable);

    Page<MaintenanceRequest> findByCompany_IdIn(List<UUID> companyIds, Pageable pageable);
}

