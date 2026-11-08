package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "tour_processing_draft_assignment_table",
        indexes = {
                @Index(name = "idx_tour_processing_draft_assignment_tour", columnList = "tour_id, part"),
                @Index(name = "idx_tour_processing_draft_assignment_driver", columnList = "driver_id"),
                @Index(name = "idx_tour_processing_draft_assignment_vehicle", columnList = "vehicle_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourProcessingDraftAssignment extends BaseEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TourProcessingDraftTour tour;

    @Column(nullable = false)
    private Integer part;

    @Column(nullable = false)
    private Integer passengers;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'INTERNAL'")
    @Column(nullable = false)
    @Builder.Default
    private ResourceType resourceType = ResourceType.INTERNAL;

    @Column(length = 200)
    private String driver;

    @Column(name = "driver_id")
    private UUID driverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", insertable = false, updatable = false)
    private Driver driverReference;

    @Column(length = 100)
    private String vehicle;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicleReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowed_vehicle_id")
    private BorrowedVehicle borrowedVehicle;

    public String getNumber() {
        return tour.getNumber();
    }

    public Integer getPriority() {
        return tour.getPriority();
    }

    public String getExecutingUnit() {
        return tour.getExecutingUnit();
    }

    public String getDestination() {
        return tour.getDestination();
    }

    public LocalDate getDepartureDate() {
        return tour.getDepartureDate();
    }

    public LocalTime getDepartureTime() {
        return tour.getDepartureTime();
    }

    public LocalDate getReturnDate() {
        return tour.getReturnDate();
    }

    public LocalTime getReturnTime() {
        return tour.getReturnTime();
    }

    public boolean isActive() {
        return tour.isPrimary() && tour.isApproved();
    }
    @jakarta.persistence.PrePersist
    void assignId() {
        if (id == null) id = UUID.randomUUID();
    }
}
