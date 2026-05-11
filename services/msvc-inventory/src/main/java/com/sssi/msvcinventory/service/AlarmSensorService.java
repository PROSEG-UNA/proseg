package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.sssi.msvcinventory.dto.response.AlarmSensorResponseDto;

import java.util.UUID;

public interface AlarmSensorService {

    AlarmSensorResponseDto create(AlarmSensorRequestDto request);

    AlarmSensorResponseDto findById(UUID id);

    AlarmSensorResponseDto update(UUID id, AlarmSensorRequestDto request);
}
