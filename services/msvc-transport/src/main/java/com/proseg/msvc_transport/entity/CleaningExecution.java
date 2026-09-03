package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cleaning_execution_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE cleaning_execution_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class CleaningExecution extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    @Column(nullable = false, length = 100)
    private String executedBy;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String fileType;

    @Column(nullable = false)
    private LocalDateTime processStartedAt;

    @Column(nullable = false)
    private LocalDateTime processFinishedAt;

    @Column(nullable = false)
    private Long durationMs;

    @Column(nullable = false)
    private Integer totalReadRecords;

    @Column(nullable = false)
    private Integer validRecords;

    @Column(nullable = false)
    private Integer invalidRecords;

    @Column(nullable = false)
    private Integer duplicatesDetected;

    @Column(nullable = false)
    private Integer createdDrivers;

    @Column(nullable = false)
    private Integer updatedDrivers;

    @Column(nullable = false)
    private Integer createdVehicles;

    @Column(nullable = false)
    private Integer updatedVehicles;

    @Column(nullable = false)
    private Integer createdTours;

    @Column(nullable = false)
    private Integer updatedTours;

    @Column(nullable = false)
    private boolean replacedExistingTours;

    private LocalDateTime replacedRangeStart;

    private LocalDateTime replacedRangeEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CleaningExecutionStatus finalStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDate executionDate;

    @OneToMany(mappedBy = "cleaningExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CleaningExecutionDetail> details = new ArrayList<>();
}
