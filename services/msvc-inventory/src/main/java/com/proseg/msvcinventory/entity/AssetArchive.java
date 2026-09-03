package com.proseg.msvcinventory.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "asset_archive_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE asset_archive_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class AssetArchive extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "object_name", nullable = false)
    private String objectName;

    private String caption;
}
