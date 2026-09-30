package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "cleaning_draft_row_table",
        indexes = @Index(name = "idx_cleaning_draft_row_draft", columnList = "draft_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE cleaning_draft_row_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class CleaningDraftRow extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private CleaningDraft draft;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer rowNumber;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 200)
    private String driver;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, length = 50)
    private String number;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 100)
    private String vehicle;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer passengers;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String executingUnit;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String responsible;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String destination;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer durationDays;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer priority;

    @Filterable(type = FilterType.TEXT)
    @Column(length = 50)
    private String modality;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private LocalTime departureTime;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private LocalTime returnTime;

    @Filterable(type = FilterType.DATE)
    @Column(nullable = false)
    private LocalDate departureDate;

    @Filterable(type = FilterType.DATE)
    @Column(nullable = false)
    private LocalDate returnDate;

    @Filterable(type = FilterType.TEXT)
    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(nullable = false)
    private Integer duplicateGroupSize;

    @Column(nullable = false)
    private Integer duplicateRank;

    @Column(nullable = false)
    private boolean conflicting;
}
