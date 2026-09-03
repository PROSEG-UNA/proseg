package com.proseg.msvcinventory.entity;

import com.proseg.common.entity.BaseEntity;
import com.proseg.common.specification.Filterable;
import com.proseg.common.specification.FilterType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "type_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE type_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Type extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Filterable(type = FilterType.TEXT)
    @Column(nullable = false)
    private String name;

    @Filterable(type = FilterType.TEXT)
    private String description;

    @Builder.Default
    @Column(name = "requires_network_interface", nullable = false)
    private boolean requiresNetworkInterface = false;
}
