package com.sssi.msvc_transport.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "cleaning_execution_detail_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE cleaning_execution_detail_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class CleaningExecutionDetail extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cleaning_execution_id", nullable = false)
    private CleaningExecution cleaningExecution;

    private Integer originalRowNumber;

    @Column(columnDefinition = "TEXT")
    private String originalData;

    @Column(columnDefinition = "TEXT")
    private String normalizedData;

    @Column(nullable = false, length = 50)
    private String recordType;

    @Column(nullable = false, length = 100)
    private String actionPerformed;

    @Column(nullable = false, length = 20)
    private String processingResult;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(columnDefinition = "TEXT")
    private String validationErrors;
}
