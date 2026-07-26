package com.sssi.msvc_transport.repository;

import com.sssi.msvc_transport.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID>, JpaSpecificationExecutor<Driver> {

    boolean existsByDocumentIdIgnoreCase(String documentId);

    boolean existsByDocumentIdIgnoreCaseAndIdNot(String documentId, UUID id);

    List<Driver> findAllByStatusIgnoreCaseAndAvailabilityTrue(String status);

    Optional<Driver> findByDocumentIdIgnoreCase(String documentId);

    Optional<Driver> findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
}
