package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, UUID>, JpaSpecificationExecutor<UserCompany> {

    boolean existsByKeycloakUserIdAndCompanyId(String keycloakUserId, UUID companyId);

    boolean existsByKeycloakUserIdAndCompanyIdAndIdNot(String keycloakUserId, UUID companyId, UUID id);

    boolean existsByCompanyIdAndKeycloakUserId(UUID companyId, String keycloakUserId);

    boolean existsByCompanyId(UUID companyId);

    Optional<UserCompany> findByCompanyIdAndKeycloakUserId(UUID companyId, String keycloakUserId);

    List<UserCompany> findAllByCompanyId(UUID companyId);

    List<UserCompany> findAllByKeycloakUserId(String keycloakUserId);

    Page<UserCompany> findByCompanyId(UUID companyId, Pageable pageable);

    @Query(value = "SELECT * FROM user_company_table WHERE company_id = :companyId AND keycloak_user_id = :keycloakUserId LIMIT 1",
            nativeQuery = true)
    Optional<UserCompany> findByCompanyIdAndKeycloakUserIdIncludingDeleted(
            @Param("companyId") UUID companyId,
            @Param("keycloakUserId") String keycloakUserId);

}

