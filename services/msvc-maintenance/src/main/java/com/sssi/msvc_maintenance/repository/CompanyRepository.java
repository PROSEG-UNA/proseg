package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByLegalIdIgnoreCase(String legalId);

    boolean existsByLegalIdIgnoreCaseAndIdNot(String legalId, UUID id);
}

