package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "driver_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE driver_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Driver extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String firstName;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String lastName;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false, unique = true)
    private String documentId;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String licenseNumber;

    @Filterable(type = FilterType.TEXT)
    private String phone;

    @Filterable(type = FilterType.TEXT)
    private String email;

    @Filterable(type = FilterType.TEXT)
    @Builder.Default
    private String status = "ACTIVE";

    @Filterable(type = FilterType.TEXT)
    @Column(columnDefinition = "TEXT")
    private String restrictions;

    @Builder.Default
    private boolean availability = true;

    @Builder.Default
    private double accumulatedHours = 0D;

    @Builder.Default
    private double freeWeekendsCount = 1D;

    @Builder.Default
    private double overtimeHours = 0D;

    @Builder.Default
    private double surplusHours = 0D;

    @Builder.Default
    private double jornadaHoursPerDay = 8D;
}
