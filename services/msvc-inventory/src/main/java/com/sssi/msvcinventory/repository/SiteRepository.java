package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiteRepository extends JpaRepository<Site, UUID>, JpaSpecificationExecutor<Site> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}