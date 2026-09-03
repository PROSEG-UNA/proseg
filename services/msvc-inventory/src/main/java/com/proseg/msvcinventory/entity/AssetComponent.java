package com.proseg.msvcinventory.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "asset_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE asset_components SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class AssetComponent extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String observations;
}

