package com.proseg.msvc_transport.repository;

import com.proseg.msvc_transport.entity.BorrowedVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowedVehicleRepository extends JpaRepository<BorrowedVehicle, UUID>, JpaSpecificationExecutor<BorrowedVehicle> {

    boolean existsByPlateIgnoreCase(String plate);

    boolean existsByPlateIgnoreCaseAndIdNot(String plate, UUID id);

    Optional<BorrowedVehicle> findByPlateIgnoreCase(String plate);
}
