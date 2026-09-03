package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "assignment_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE assignment_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @Filterable(type = FilterType.TEXT, nestedPaths = {"id", "firstName", "lastName", "documentId"})
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @Filterable(type = FilterType.TEXT, nestedPaths = {"id", "plate", "brand", "model"})
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    @Filterable(type = FilterType.TEXT, nestedPaths = {"id", "name", "origin", "destination", "status"})
    private Tour tour;

    @Filterable(type = FilterType.TEXT)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean isContracted = false;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
}
