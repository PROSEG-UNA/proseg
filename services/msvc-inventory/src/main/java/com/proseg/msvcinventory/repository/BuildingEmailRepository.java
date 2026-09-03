package com.proseg.msvcinventory.repository;

import com.proseg.msvcinventory.entity.BuildingEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BuildingEmailRepository extends JpaRepository<BuildingEmail, UUID> {

    List<BuildingEmail> findByBuildingId(UUID buildingId);

    List<BuildingEmail> findByBuildingCampusId(UUID campusId);

    boolean existsByBuildingIdAndEmail(UUID buildingId, String email);
}