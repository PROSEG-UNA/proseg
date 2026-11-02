package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "tour_processing_draft_tour_table",
        indexes = @Index(name = "idx_tour_processing_draft_tour_draft", columnList = "draft_id, row_number")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingDraftTour extends BaseEntity {

    private static final String PRIMARY_ASSIGNMENT = "from tour_processing_draft_assignment_table a where a.tour_id = id and a.part = 1";

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private TourProcessingDraft draft;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private Integer rowNumber;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, length = 50)
    private String number;

    @Filterable(type = FilterType.TEXT)
    @Formula("(select a.driver " + PRIMARY_ASSIGNMENT + ")")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String driver;

    @Filterable(type = FilterType.TEXT)
    @Formula("(select a.vehicle " + PRIMARY_ASSIGNMENT + ")")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String vehicle;

    @Filterable(type = FilterType.ENUM)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'APPROVED'")
    @Column(nullable = false)
    @Builder.Default
    private TourApprovalStatus approvalStatus = TourApprovalStatus.APPROVED;

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
    @Builder.Default
    private Integer duplicateGroupSize = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer duplicateRank = 1;

    @Column(nullable = false)
    private boolean conflicting;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("part ASC")
    @Builder.Default
    private List<TourProcessingDraftAssignment> assignments = new ArrayList<>();

    public boolean isPrimary() {
        return duplicateRank == null || duplicateRank == 1;
    }

    public boolean isApproved() {
        return approvalStatus != TourApprovalStatus.NOT_APPROVED;
    }

    public boolean isDistributed() {
        return assignments.size() > 1;
    }

    public TourProcessingDraftAssignment primaryAssignment() {
        return assignments.isEmpty() ? null : assignments.get(0);
    }

    public TourProcessingDraftAssignment addAssignment(TourProcessingDraftAssignment assignment) {
        assignment.setTour(this);
        assignment.setPart(assignments.size() + 1);
        assignments.add(assignment);
        return assignment;
    }
    @jakarta.persistence.PrePersist
    void assignId() {
        if (id == null) id = UUID.randomUUID();
    }
}
