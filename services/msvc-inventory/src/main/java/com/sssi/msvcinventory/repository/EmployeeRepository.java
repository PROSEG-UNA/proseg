package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findFirstByNameIgnoreCase(String name);

    Optional<Employee> findFirstByIdentificationIgnoreCase(String identification);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByIdentificationIgnoreCase(String identification);

    boolean existsByIdentificationIgnoreCaseAndIdNot(String identification, UUID id);
}
