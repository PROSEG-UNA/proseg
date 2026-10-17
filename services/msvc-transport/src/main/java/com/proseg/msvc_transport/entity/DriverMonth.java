package com.proseg.msvc_transport.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "driver_month_table",
        uniqueConstraints = @UniqueConstraint(name = "uk_driver_month", columnNames = {"driver_id", "year", "month"}),
        indexes = @Index(name = "idx_driver_month_driver", columnList = "driver_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE driver_month_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class DriverMonth extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Builder.Default
    @Column(nullable = false)
    private Double ordinaryHoursAccrued = 0D;

    @Builder.Default
    @Column(nullable = false)
    private Double surplusHoursAccrued = 0D;

    @Builder.Default
    @Column(nullable = false)
    private Double overtimeHoursAccrued = 0D;

    @Builder.Default
    @Column(nullable = false)
    private Integer weekendsWorked = 0;
}
