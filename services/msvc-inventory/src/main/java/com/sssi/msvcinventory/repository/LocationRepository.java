package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Location;
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
public interface LocationRepository extends JpaRepository<Location, UUID>, JpaSpecificationExecutor<Location> {

    @EntityGraph("Location.withRelations")
    Page<Location> findAll(Specification<Location> spec, Pageable pageable);

    @EntityGraph("Location.withRelations")
    Optional<Location> findById(UUID id);

    Optional<Location> findFirstByDescriptionIgnoreCaseAndFloorId(String description, UUID floorId);

    @EntityGraph("Location.withRelations")
    List<Location> findByDescriptionIgnoreCase(String description);

    @EntityGraph("Location.withRelations")
    Optional<Location> findFirstByDescriptionIgnoreCaseAndFloorBuildingId(String description, UUID buildingId);

    List<Location> findByFloorId(UUID floorId);

    boolean existsByFloorId(UUID floorId);

    boolean existsByDescriptionIgnoreCaseAndFloorId(String description, UUID floorId);

    boolean existsByDescriptionIgnoreCaseAndFloorIdAndIdNot(String description, UUID floorId, UUID id);

    @EntityGraph("Location.withRelations")
    Page<Location> findByFloorId(UUID floorId, Pageable pageable);

    @EntityGraph("Location.withRelations")
    Page<Location> findByFloorBuildingId(UUID buildingId, Pageable pageable);

    @EntityGraph("Location.withRelations")
    Page<Location> findByFloorBuildingCampusId(UUID campusId, Pageable pageable);
}
