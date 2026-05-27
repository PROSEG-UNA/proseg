package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID>, JpaSpecificationExecutor<Location> {

    List<Location> findByFloorId(UUID floorId);

    boolean existsByFloorId(UUID floorId);

    boolean existsByDescriptionIgnoreCaseAndFloorId(String description, UUID floorId);

    boolean existsByDescriptionIgnoreCaseAndFloorIdAndIdNot(String description, UUID floorId, UUID id);

    Page<Location> findByFloorId(UUID floorId, Pageable pageable);

    Page<Location> findByFloorBuildingId(UUID buildingId, Pageable pageable);

    Page<Location> findByFloorBuildingCampusId(UUID campusId, Pageable pageable);
}
