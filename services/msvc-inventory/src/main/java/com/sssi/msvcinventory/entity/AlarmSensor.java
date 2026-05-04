package com.sssi.msvcinventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alarm_sensor_table")
@DiscriminatorValue("ALARM_SENSOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlarmSensor extends Asset {

    @Column(nullable = false)
    private String panel;

    @Column(name = "sensor_type", nullable = false)
    private String sensorType;

    @Column(name = "linked_output")
    private String linkedOutput;
}
