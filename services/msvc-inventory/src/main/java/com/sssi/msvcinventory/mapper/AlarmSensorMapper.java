package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.sssi.msvcinventory.dto.response.AlarmSensorResponseDto;
import com.sssi.msvcinventory.entity.AlarmSensor;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {ModelMapper.class, LocationMapper.class, NetworkInterfaceMapper.class})
public interface AlarmSensorMapper {

    @Mapping(target = "model", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "networkInterface", ignore = true)
    AlarmSensor toEntity(AlarmSensorRequestDto request);

    @Mapping(target = "kind", constant = "ALARM_SENSOR")
    AlarmSensorResponseDto toResponse(AlarmSensor alarmSensor);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "model", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "networkInterface", ignore = true)
    void updateEntityFromRequest(AlarmSensorRequestDto request, @MappingTarget AlarmSensor alarmSensor);
}