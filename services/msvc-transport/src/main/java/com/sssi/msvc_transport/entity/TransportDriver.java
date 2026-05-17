package com.sssi.msvc_transport.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "transport_driver_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE transport_driver_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class TransportDriver extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "has_medical_restriction")
    private Boolean hasMedicalRestriction;

    @Column(name = "medical_restriction", length = 2000)
    private String medicalRestriction;

    @Column(name = "fixed_priority")
    private Integer fixedPriority;

    @Column(name = "observations", length = 2000)
    private String observations;
}