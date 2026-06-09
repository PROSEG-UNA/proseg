package com.sssi.msvc_maintenance.repository;

import com.sssi.msvc_maintenance.entity.MaintenanceEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceEmailRepository extends JpaRepository<MaintenanceEmail, UUID> {

    Optional<MaintenanceEmail> findByEmail(String email);
}
