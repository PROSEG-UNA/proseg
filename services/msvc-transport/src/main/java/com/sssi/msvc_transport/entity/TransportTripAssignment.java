package com.sssi.msvc_transport.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transport_trip_assignment_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE transport_trip_assignment_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class TransportTripAssignment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transport_trip_detail_id", nullable = false)
    private TransportTripDetail transportTripDetail;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "vehicle_plate")
    private String vehiclePlate;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "assignment_type")
    private String assignmentType;

    @Column(name = "assignment_status")
    private String assignmentStatus;

    @Column(name = "regular_hours")
    private BigDecimal regularHours;

    @Column(name = "overtime_hours")
    private BigDecimal overtimeHours;

    @Column(name = "extra_hours")
    private BigDecimal extraHours;

    @Column(name = "is_external_contract")
    private Boolean isExternalContract;

    @Column(name = "is_vehicle_loan")
    private Boolean isVehicleLoan;

    @Column(name = "is_rejected")
    private Boolean isRejected;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "observations", length = 2000)
    private String observations;
}