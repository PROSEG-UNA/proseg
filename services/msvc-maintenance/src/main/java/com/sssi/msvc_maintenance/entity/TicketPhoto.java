package com.sssi.msvc_maintenance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "ticket_photo_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPhoto {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "object_name")
    private String objectName;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;
}
