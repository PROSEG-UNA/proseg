package com.sssi.msvcinventory.entity;

import com.sssi.common.entity.BaseEntity;
import com.sssi.msvcinventory.entity.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "asset_table")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "asset_kind", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("GENERIC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE asset_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String subtype;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_model_id", nullable = false)
    private AssetModel assetModel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    @Column(name = "status_description")
    private String statusDescription;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "firmware_support_end_date")
    private LocalDate firmwareSupportEndDate;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL)
    private NetworkInterface networkInterface;
}