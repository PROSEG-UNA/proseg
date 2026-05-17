package com.sssi.msvc_transport.entity;

import com.sssi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "transport_trip_archive_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE transport_trip_table SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class TransportTripArchive extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "object_name", nullable = false)
    private String objectName;

    @Column(name = "status")
    private String status;
}