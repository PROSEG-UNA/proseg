package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.ExternalVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalVehicleRepository extends JpaRepository<ExternalVehicle, UUID>, JpaSpecificationExecutor<ExternalVehicle> {

    boolean existsByPlateIgnoreCaseAndIdNot(String plate, UUID id);

    Optional<ExternalVehicle> findByPlateIgnoreCase(String plate);

    List<ExternalVehicle> findAllByIsDeletedFalse();

    Optional<ExternalVehicle> findByIdAndIsDeletedFalse(UUID id);
}
