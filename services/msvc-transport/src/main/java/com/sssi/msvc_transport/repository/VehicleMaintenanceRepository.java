package com.sssi.msvc_transport.repository;

import com.sssi.msvc_transport.entity.VehicleMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, UUID>, JpaSpecificationExecutor<VehicleMaintenance> {

    boolean existsByVehicleIdAndStatusIn(UUID vehicleId, Collection<String> statuses);

    long countByVehicleId(UUID vehicleId);
}
