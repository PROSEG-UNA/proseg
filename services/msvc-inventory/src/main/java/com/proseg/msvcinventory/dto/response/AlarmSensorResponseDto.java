package com.proseg.msvcinventory.dto.response;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;

@JsonTypeName("ALARM_SENSOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AlarmSensorResponseDto extends AssetResponseDto {

    private String panel;
    private String sensorType;
    private String linkedOutput;
}
