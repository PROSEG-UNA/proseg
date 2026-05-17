package com.sssi.msvc_transport.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "transport_trip_detail_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE transport_trip_detail_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class TransportTripDetail extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transport_trip_id", nullable = false)
    private TransportTripArchive transportTripArchive;

    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    @Column(name = "request_number")
    private String requestNumber;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "requested_vehicle")
    private String requestedVehicle;

    @Column(name = "passenger_count")
    private Integer passengerCount;

    @Column(name = "executing_unit")
    private String executingUnit;

    @Column(name = "responsible_person")
    private String responsiblePerson;

    @Column(name = "destination")
    private String destination;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "modality")
    private String modality;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @Column(name = "observations", length = 2000)
    private String observations;

    @Column(name = "is_duplicate")
    private Boolean isDuplicate;

    @Column(name = "is_valid")
    private Boolean isValid;

    @Column(name = "validation_message", length = 2000)
    private String validationMessage;
}