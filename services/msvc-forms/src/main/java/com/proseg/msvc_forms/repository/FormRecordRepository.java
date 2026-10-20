package com.proseg.msvc_forms.repository;

import com.proseg.msvc_forms.entity.FormRecord;
import com.proseg.msvc_forms.entity.FormType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormRecordRepository extends JpaRepository<FormRecord, UUID> {

    Optional<FormRecord> findByIdAndIsDeletedFalse(UUID id);

    Page<FormRecord> findByFormTypeAndIsDeletedFalse(FormType formType, Pageable pageable);

    @Query("SELECT fr FROM FormRecord fr WHERE fr.isDeleted = false " +
           "AND (:formTypeId IS NULL OR fr.formType.id = :formTypeId) " +
           "AND (:createdBy IS NULL OR fr.createdBy = :createdBy) " +
           "ORDER BY fr.createdAt DESC")
    Page<FormRecord> findWithFilters(
            @Param("formTypeId") UUID formTypeId,
            @Param("createdBy") String createdBy,
            Pageable pageable);

    @Query("SELECT fr FROM FormRecord fr WHERE fr.isDeleted = false " +
           "AND fr.formType.code = :formTypeCode " +
           "ORDER BY fr.createdAt DESC")
    Page<FormRecord> findByFormTypeCode(
            @Param("formTypeCode") String formTypeCode,
            Pageable pageable);
}
