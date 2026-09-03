package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByPlateIgnoreCase(String plate);

    boolean existsByPlateIgnoreCaseAndIdNot(String plate, UUID id);

    List<Vehicle> findAllByStatusIgnoreCaseAndAvailableTrueAndUnderMaintenanceFalse(String status);

    Optional<Vehicle> findByPlateIgnoreCase(String plate);
}
