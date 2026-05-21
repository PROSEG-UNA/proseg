package com.sssi.msvc_maintenance.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "maintenance_technician_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE maintenance_technician_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class MaintenanceTechnician extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 100)
    private String position;

    private String email;

    @Column(length = 50)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private boolean leader = false;
}