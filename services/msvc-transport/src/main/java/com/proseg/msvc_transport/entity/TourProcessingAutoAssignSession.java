package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
        name = "tour_processing_auto_assign_session_table",
        uniqueConstraints = @UniqueConstraint(name = "uk_tour_processing_auto_assign_session_draft", columnNames = "draft_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingAutoAssignSession extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "draft_id", nullable = false)
    private UUID draftId;

    @Column(name = "current_step", length = 60)
    private String currentStep;

    @Column(name = "scope_tour_ids", nullable = false, columnDefinition = "TEXT")
    private String scopeTourIds;

    @Column(name = "snapshot", columnDefinition = "TEXT")
    private String snapshot;

    @Column(name = "calculated_fingerprint", length = 64)
    private String calculatedFingerprint;

    @Column(name = "last_result", columnDefinition = "TEXT")
    private String lastResult;
}
