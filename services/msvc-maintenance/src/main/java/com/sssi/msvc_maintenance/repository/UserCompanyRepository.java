package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, UUID>, JpaSpecificationExecutor<UserCompany> {

    boolean existsByKeycloakUserIdAndCompanyId(String keycloakUserId, UUID companyId);

    boolean existsByKeycloakUserIdAndCompanyIdAndIdNot(String keycloakUserId, UUID companyId, UUID id);

    boolean existsByCompanyId(UUID companyId);

    Page<UserCompany> findByCompanyId(UUID companyId, Pageable pageable);
}

