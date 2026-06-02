package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID>, JpaSpecificationExecutor<Floor> {

    Optional<Floor> findByNameAndBuildingId(String name, UUID buildingId);

    boolean existsByBuildingId(UUID buildingId);
}
