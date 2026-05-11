package com.sssi.msvcinventory.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sssi.msvcinventory.entity.enums.AssetStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlarmSensorResponseDto.class, name = "ALARM_SENSOR"),
        @JsonSubTypes.Type(value = AssetResponseDto.class, name = "GENERIC"),
        @JsonSubTypes.Type(value = AssetResponseDto.class, name = "CAMERA")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AssetResponseDto {

    private UUID id;
    private String kind;
    private String name;
    private String description;
    private ModelResponseDto model;
    private LocationResponseDto location;
    private AssetStatus status;
    private String statusDescription;
    private LocalDate acquisitionDate;
    private LocalDate warrantyEndDate;
    private LocalDate firmwareSupportEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> archiveUrls;
    private NetworkInterfaceResponseDto networkInterface;
}