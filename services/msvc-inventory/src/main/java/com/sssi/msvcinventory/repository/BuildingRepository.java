package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID>, JpaSpecificationExecutor<Building> {

    @EntityGraph("Building.withCampus")
    Page<Building> findAll(Specification<Building> spec, Pageable pageable);

    @EntityGraph("Building.withCampus")
    Optional<Building> findById(UUID id);

    Optional<Building> findFirstByNameIgnoreCaseAndCampusId(String name, UUID campusId);

    @EntityGraph("Building.withCampus")
    List<Building> findByNameIgnoreCase(String name);

    List<Building> findByCampusId(UUID campusId);

    boolean existsByNameIgnoreCaseAndCampusId(String name, UUID campusId);

    boolean existsByNameIgnoreCaseAndCampusIdAndIdNot(String name, UUID campusId, UUID id);

    boolean existsByCampusId(UUID campusId);

    @EntityGraph("Building.withCampus")
    Page<Building> findByCampusId(UUID campusId, Pageable pageable);
}
