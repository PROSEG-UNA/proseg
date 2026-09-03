package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.proseg.msvcinventory.dto.response.AlarmSensorResponseDto;
import com.proseg.msvcinventory.entity.AlarmSensor;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {ModelMapper.class, LocationMapper.class, NetworkInterfaceMapper.class,
        ExecutingUnitMapper.class, EmployeeMapper.class})
public interface AlarmSensorMapper {

    @Mapping(target = "model", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "networkInterface", ignore = true)
    @Mapping(target = "executingUnit", ignore = true)
    @Mapping(target = "employee", ignore = true)
    AlarmSensor toEntity(AlarmSensorRequestDto request);

    @Mapping(target = "kind", constant = "ALARM_SENSOR")
    AlarmSensorResponseDto toResponse(AlarmSensor alarmSensor);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "model", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "networkInterface", ignore = true)
    @Mapping(target = "executingUnit", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "serialNumber", source = "serialNumber",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void updateEntityFromRequest(AlarmSensorRequestDto request, @MappingTarget AlarmSensor alarmSensor);
}