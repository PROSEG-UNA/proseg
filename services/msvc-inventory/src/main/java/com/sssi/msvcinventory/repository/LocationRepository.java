package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findBySiteId(UUID siteId);

    boolean existsBySiteId(UUID siteId);

    boolean existsByNameIgnoreCaseAndSiteId(String name, UUID siteId);

    boolean existsByNameIgnoreCaseAndSiteIdAndIdNot(String name, UUID siteId, UUID id);

    Page<Location> findBySiteId(UUID siteId, Pageable pageable);
}