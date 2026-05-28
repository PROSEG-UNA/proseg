package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID>, JpaSpecificationExecutor<Building> {

    boolean existsByNameIgnoreCaseAndCampusId(String name, UUID campusId);

    boolean existsByNameIgnoreCaseAndCampusIdAndIdNot(String name, UUID campusId, UUID id);

    boolean existsByCampusId(UUID campusId);

    Page<Building> findByCampusId(UUID campusId, Pageable pageable);
}
