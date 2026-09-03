package com.proseg.msvcinventory.repository;

import com.proseg.msvcinventory.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TypeRepository extends JpaRepository<Type, UUID>, JpaSpecificationExecutor<Type> {

    Optional<Type> findFirstByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
