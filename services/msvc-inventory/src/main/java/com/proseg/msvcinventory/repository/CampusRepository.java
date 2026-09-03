package com.proseg.msvcinventory.repository;

import com.proseg.msvcinventory.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampusRepository extends JpaRepository<Campus, UUID>, JpaSpecificationExecutor<Campus> {

    Optional<Campus> findFirstByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
