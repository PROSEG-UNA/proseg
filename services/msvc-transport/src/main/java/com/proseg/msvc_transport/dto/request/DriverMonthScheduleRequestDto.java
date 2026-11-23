package com.proseg.msvc_transport.dto.request;

import com.proseg.msvc_transport.dto.response.DriverDayScheduleDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverMonthScheduleRequestDto {
    private List<DriverDayScheduleDto> days;
}
