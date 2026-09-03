package com.proseg.msvcinventory.mapper;

import com.proseg.msvcinventory.dto.request.EmployeeRequestDto;
import com.proseg.msvcinventory.dto.response.EmployeeResponseDto;
import com.proseg.msvcinventory.entity.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    Employee toEntity(EmployeeRequestDto request);

    EmployeeResponseDto toResponse(Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(EmployeeRequestDto request, @MappingTarget Employee employee);
}
