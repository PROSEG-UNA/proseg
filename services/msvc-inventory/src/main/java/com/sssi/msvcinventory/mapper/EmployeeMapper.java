package com.sssi.msvcinventory.mapper;

import com.sssi.msvcinventory.dto.request.EmployeeRequestDto;
import com.sssi.msvcinventory.dto.response.EmployeeResponseDto;
import com.sssi.msvcinventory.entity.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    Employee toEntity(EmployeeRequestDto request);

    EmployeeResponseDto toResponse(Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(EmployeeRequestDto request, @MappingTarget Employee employee);
}
