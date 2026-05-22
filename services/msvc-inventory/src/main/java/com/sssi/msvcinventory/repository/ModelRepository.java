package com.sssi.msvcinventory.repository;

import com.sssi.msvcinventory.entity.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ModelRepository extends JpaRepository<Model, UUID>, JpaSpecificationExecutor<Model> {

    boolean existsByNameIgnoreCaseAndBrandId(String name, UUID brandId);

    boolean existsByNameIgnoreCaseAndBrandIdAndIdNot(String name, UUID brandId, UUID id);

    boolean existsByTypeId(UUID typeId);

    boolean existsByBrandId(UUID brandId);

    Page<Model> findByBrandId(UUID brandId, Pageable pageable);

    Page<Model> findByTypeId(UUID typeId, Pageable pageable);
}