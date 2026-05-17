package com.sssi.msvc_transport.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transport_vehicle_maintenance_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE transport_vehicle_maintenance_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class TransportVehicleMaintenance extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transport_vehicle_id", nullable = false)
    private TransportVehicle transportVehicle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "observations", length = 2000)
    private String observations;
}