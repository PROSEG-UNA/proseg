package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
import java.util.UUID;

@Entity
@Table(name = "cleaning_draft_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE cleaning_draft_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class CleaningDraft extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, length = 255)
    private String fileName;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, length = 10)
    private String fileType;

    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CleaningDraftStatus status;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, length = 100)
    private String importedBy;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private LocalDateTime importedAt;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer totalRows;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer duplicateRows;

    @Column(nullable = false)
    private Integer conflictingRows;

    @Column(nullable = false)
    private Integer blocksDetected;

    private LocalDate minDepartureDate;

    private LocalDate maxReturnDate;

    private LocalDateTime registeredAt;

    private UUID cleaningExecutionId;
}
